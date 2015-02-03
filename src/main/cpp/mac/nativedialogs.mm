

// cocoa includes
#import <Foundation/Foundation.h>
#import <Cocoa/Cocoa.h>

#import <jni.h>
#import <jawt_md.h>
#import <JavaNativeFoundation/JavaNativeFoundation.h>

#include "../jniload.h"
#include "nativedialogs.h"

#define RESULT_OK 0x01
#define RESULT_CANCEL 0x02
#define ANSWER_YES 0x01
#define ANSWER_NO 0x03
#define RESULT_UNKNOWN 0x04

/**
 * Get Cocoa window references
 */
// Given a Java component, return a NSWindow*
NSWindow *convertToNSWindow(JNIEnv *env, jobject window) {
	JNF_COCOA_ENTER(env);

	JAWT awt;
	JAWT_DrawingSurface* ds;
	JAWT_DrawingSurfaceInfo* dsi;
	jboolean result;
	jint lock;
	NSWindow *retVal = nil;
    
	// Get the AWT
#ifdef JAWT_MACOSX_USE_CALAYER
	awt.version = JAWT_VERSION_1_4 | JAWT_MACOSX_USE_CALAYER;
#else
	awt.version = JAWT_VERSION_1_4;
#endif

	result = JAWT_GetAWT(env, &awt);
	if(result == JNI_FALSE) 
		return NULL;
    
	// Get the drawing surface
	ds = awt.GetDrawingSurface(env, window);
	assert(ds != NULL);
    
	// Lock the drawing surface
	lock = ds->Lock(ds);
	assert((lock & JAWT_LOCK_ERROR) == 0);
    
	// Get the drawing surface info
	dsi = ds->GetDrawingSurfaceInfo(ds);
    
#ifdef JAWT_MACOSX_USE_CALAYER
	id<JAWT_SurfaceLayers> dsi_mac;
	// Get the platform-specific drawing info
	dsi_mac = (id<JAWT_SurfaceLayers>)dsi->platformInfo;
    
    CALayer *windowLayer = [dsi_mac windowLayer];
	retVal = nil;
	NSArray *windowList = [NSApp windows];
	for(int i = 0; i < [windowList count]; i++) {
		NSWindow *window = (NSWindow*)[windowList objectAtIndex:i];
		CALayer *clayer = [[window contentView] layer];
		if(clayer != nil) {
			if(clayer == windowLayer || clayer == [windowLayer superlayer]) {
				retVal = window;
				break;
			}
		}
	}
#else
	JAWT_MacOSXDrawingSurfaceInfo* dsi_mac;
	
	dsi_mac = (JAWT_MacOSXDrawingSurfaceInfo*)dsi->platformInfo;
	
	// Get the NSView corresponding to the component that was passed
	NSView *view = dsi_mac->cocoaViewRef;
	retVal = (view == nil ? nil : [view window]);
#endif

	// Free the drawing surface info
	ds->FreeDrawingSurfaceInfo(dsi);
	// Unlock the drawing surface
	ds->Unlock(ds);
    
	// Free the drawing surface
	awt.FreeDrawingSurface(ds);

	// Get the view's parent window; this is what we need to show a sheet
	return retVal;
	
	JNF_COCOA_EXIT(env)
}

/**
 * Create a new dialog result
 */
jobject createDialogResult(JNIEnv *env, int result, jobject data) {
	const char *szClassName = "ca/phon/ui/nativedialogs/NativeDialogEvent";
	const char *szCstrSig = "(ILjava/lang/Object;)V";
	jobject retVal = NULL;
	
	jclass NativeDialogEvent = env->FindClass(szClassName);
	jmethodID cstr = env->GetMethodID(NativeDialogEvent, "<init>", szCstrSig);
	
	retVal = env->NewObject(NativeDialogEvent, cstr, result, data);
	
	return retVal;
}

void sendDialogResult(JNIEnv *env, jobject listener, jobject result) {
	const char *szClassName = "ca/phon/ui/nativedialogs/NativeDialogListener";
	const char *szMethodName = "nativeDialogEvent";
	const char *szMethodSig = "(Lca/phon/ui/nativedialogs/NativeDialogEvent;)V";
	
	jclass NativeDialogListener = env->FindClass(szClassName);
	jmethodID methodId = env->GetMethodID(NativeDialogListener, szMethodName, szMethodSig);

	env->CallVoidMethod(listener, methodId, result);
}

/**
 * Process a list of filters into an NSArray of file extensions
 */
NSArray *GetAllowedFiletypes(JNIEnv *env, jobject filefilter) {
	const char *szClassName = "ca/phon/ui/nativedialogs/FileFilter";
	const char *szMethodSig = "()Ljava/util/List;";
	const char *szMethodName = "getAllExtensions";

	const char *szListClass = "java/util/List";
	const char *szGetMethod = "get";
	const char *szGetMethodSig = "(I)Ljava/lang/Object;";
	const char *szSizeMethod = "size";
	const char *szSizeMethodSig = "()I";
		
	jclass FileFilter = env->FindClass(szClassName);
	jmethodID exts = env->GetMethodID(FileFilter, szMethodName, szMethodSig);
	
	jclass List = env->FindClass(szListClass);
	jmethodID get = env->GetMethodID(List, szGetMethod, szGetMethodSig);
	jmethodID size = env->GetMethodID(List, szSizeMethod, szSizeMethodSig);
	
	NSMutableArray *retVal = [[NSMutableArray alloc] init];
	
	jobject extList = env->CallObjectMethod(filefilter, exts);
	int count = env->CallIntMethod(extList, size);
	
	for(int i = 0; i < count; i++) {
		jstring ext = (jstring)env->CallObjectMethod(extList, get, i);
		[retVal addObject:JNFJavaToNSString(env, ext)];
	}
	
	return retVal;
}

/**
 * Retrive an object from a property map
 */
 jobject GetProperty(JNIEnv* env, jobject props, jstring propName) {
	const char *szMethodName = "get";
	const char *szMethodSig = "(Ljava/lang/Object;)Ljava/lang/Object;";
	
	jclass clazz = env->GetObjectClass(props);
	jmethodID get = env->GetMethodID(clazz, szMethodName, szMethodSig);
	
	return env->CallObjectMethod(props, get, propName);
}

jobject GetProperty(JNIEnv *env, jobject props, NSString *propName) {
	return GetProperty(env, props, JNFNSToJavaString(env, propName));
}

bool GetBool(JNIEnv *env, jobject obj) {
	const char *szMethodName = "booleanValue";
	const char *szMethodSig = "()Z";
	
	jclass clazz = env->GetObjectClass(obj);
	jmethodID methodID = env->GetMethodID(clazz, szMethodName, szMethodSig);
	
	return env->CallBooleanMethod(obj, methodID);
}

jobject ToBool(JNIEnv* env, bool val) {
	const char *szClassName = "java/lang/Boolean";
	const char *szCstrSig = "(Z)V";
	
	jclass clazz = env->FindClass(szClassName);
	jmethodID cstrID = env->GetMethodID(clazz, "<init>", szCstrSig);
	
	return env->NewObject(clazz, cstrID, val);
}

/*
 * Class:     ca_phon_ui_nativedialogs_NativeDialogs
 * Method:    nativeShowOpenDialog
 * Signature: (Lca/phon/ui/nativedialogs/OpenDialogProperties;)V
 */
JNIEXPORT void JNICALL Java_ca_phon_ui_nativedialogs_NativeDialogs_nativeShowOpenDialog
 	(JNIEnv *env, jclass clazz, jobject props) {
 	JNF_COCOA_ENTER(env)
  	
	// get all relevant properties
	jobject titleObj = GetProperty(env, props, @"title");
	NSString *title = (titleObj ? JNFJavaToNSString(env, (jstring)titleObj) : nil);
	
	jobject promptObj = GetProperty(env, props, @"prompt");
	NSString *prompt = (promptObj ? JNFJavaToNSString(env, (jstring)promptObj) : nil); 
	
	jobject nflObj = GetProperty(env, props, @"name_field_label");
	NSString *nameFieldLabel = (nflObj ? JNFJavaToNSString(env, (jstring)nflObj) : nil);
	
	jobject parentWindowObj = GetProperty(env, props, @"parent_window");
	NSWindow *parentWindow = (parentWindowObj ? convertToNSWindow(env, parentWindowObj) : nil);
	
	jobject initialFolderObj = GetProperty(env, props, @"initial_folder");
	NSString *initialFolder = (initialFolderObj ? JNFJavaToNSString(env, (jstring)initialFolderObj) : nil);
	
	jobject filenameObj = GetProperty(env, props, @"initial_file");
	NSString *filename = (filenameObj ? JNFJavaToNSString(env, (jstring)filenameObj) : nil);
	
	jobject filterObj = GetProperty(env, props, @"file_filter");
	NSArray *nsFilters = (filterObj ? GetAllowedFiletypes(env, filterObj) : nil);
	
	jobject messageObj = GetProperty(env, props, @"message");
	NSString *message = (messageObj ? JNFJavaToNSString(env, (jstring)messageObj) : nil);
	
	jobject canCreateFoldersObj = GetProperty(env, props, @"can_create_directories");
	bool canCreateFolders = (canCreateFoldersObj ? GetBool(env, canCreateFoldersObj) : false);
	
	jobject showHiddenObj = GetProperty(env, props, @"show_hidden");
	bool showHidden = (showHiddenObj ? GetBool(env, showHiddenObj) : false);
	
	jobject canSelectFilesObj = GetProperty(env, props, @"can_choose_files");
	bool canSelectFiles = (canSelectFilesObj ? GetBool(env, canSelectFilesObj) : nil);
	
	jobject canSelectFoldersObj = GetProperty(env, props, @"can_choose_directories");
	bool canSelectFolders = (canSelectFoldersObj ? GetBool(env, canSelectFoldersObj) : nil);
	
	jobject allowMultipleSelectionObj = GetProperty(env, props, @"allow_multiple_selection");
	bool allowMultipleSelection = (allowMultipleSelectionObj ? GetBool(env, allowMultipleSelectionObj) : nil);
	
	jobject gListener = env->NewGlobalRef(GetProperty(env, props, @"listener"));
	
    void (^block)(void);
    block = ^(void){
        NSOpenPanel *openPanel = [[NSOpenPanel openPanel] retain];
        [openPanel setCanCreateDirectories:canCreateFolders];
        [openPanel setShowsHiddenFiles:showHidden];
        [openPanel setCanChooseFiles:canSelectFiles];
        [openPanel setCanChooseDirectories:canSelectFolders];
        [openPanel setAllowsMultipleSelection:allowMultipleSelection];
		
		if(title)
			[openPanel setTitle:title];
			
		if(nsFilters)
			[openPanel setAllowedFileTypes:nsFilters];
			
		if(initialFolder) {
			NSURL *nsStartURL = [NSURL fileURLWithPath:initialFolder];
			[openPanel setDirectoryURL:nsStartURL];
		}
		
		if(filename) {
			[openPanel setNameFieldStringValue:filename];
		}
		
		if(prompt) {
			[openPanel setPrompt:prompt];
		}
		
		if(nameFieldLabel) {
			[openPanel setNameFieldLabel:nameFieldLabel];
		}
		
		if(message) {
			[openPanel setMessage:message];
		}
		
		void (^handler)(NSInteger) = ^(NSInteger result){
			
			bool detach = false;
			JNIEnv *env = NULL;
			GetJNIEnv(&env, &detach);
			jobject data = NULL;
			
			if (result == NSFileHandlingPanelOKButton) {
				if(!allowMultipleSelection) {
		        	NSURL *selectedURL = [openPanel URL];
		        	data = JNFNSToJavaString(env, [selectedURL path]);
		        } else {
		        	NSArray *urls = [openPanel URLs];
		        	jclass String = env->FindClass("java/lang/String");
		        	
		        	jobjectArray pathArray = env->NewObjectArray([urls count], String, 0);
		        	for(int i = 0; i < [urls count]; i++) {
		        		env->SetObjectArrayElement(pathArray, i, JNFNSToJavaString(env, [[urls objectAtIndex:i] path]));
		        	}
		        	
		        	data = pathArray;
		        }
		    }

			[openPanel orderOut:nil];
			[openPanel release];
			
	    	jobject dlgevt = NULL;
		    if(data != nil) {
		    	dlgevt = createDialogResult(env, RESULT_OK, data);
		    } else {
		    	dlgevt = createDialogResult(env, RESULT_CANCEL, NULL);
		    }
		
			sendDialogResult(env, gListener, dlgevt);
			
			env->DeleteGlobalRef(gListener);
		    
		};
		
		if(parentWindow) {
			[openPanel beginSheetModalForWindow:parentWindow completionHandler:handler];
		} else {
			[openPanel beginWithCompletionHandler:handler];
		}
		
    };
    
    if ([NSThread isMainThread]){
        block();
    } else {
        [JNFRunLoop performOnMainThreadWaiting:YES withBlock:block];
    }
    
	JNF_COCOA_EXIT(env)
}

/*
 * Class:     ca_phon_ui_nativedialogs_NativeDialogs
 * Method:    nativeShowSaveFileDialog
 * Signature: (Lca/phon/ui/nativedialogs/SaveDialogProperties;)V
 */
JNIEXPORT void JNICALL Java_ca_phon_ui_nativedialogs_NativeDialogs_nativeShowSaveDialog
  (JNIEnv *env, jclass clazz, jobject props) {
  	JNF_COCOA_ENTER(env)
  	
	// get all relevant properties
	jobject titleObj = GetProperty(env, props, @"title");
	NSString *title = (titleObj ? JNFJavaToNSString(env, (jstring)titleObj) : nil);
	
	jobject promptObj = GetProperty(env, props, @"prompt");
	NSString *prompt = (promptObj ? JNFJavaToNSString(env, (jstring)promptObj) : nil); 
	
	jobject nflObj = GetProperty(env, props, @"name_field_label");
	NSString *nameFieldLabel = (nflObj ? JNFJavaToNSString(env, (jstring)nflObj) : nil);
	
	jobject parentWindowObj = GetProperty(env, props, @"parent_window");
	NSWindow *parentWindow = (parentWindowObj ? convertToNSWindow(env, parentWindowObj) : nil);
	
	jobject initialFolderObj = GetProperty(env, props, @"initial_folder");
	NSString *initialFolder = (initialFolderObj ? JNFJavaToNSString(env, (jstring)initialFolderObj) : nil);
	
	jobject filenameObj = GetProperty(env, props, @"initial_file");
	NSString *filename = (filenameObj ? JNFJavaToNSString(env, (jstring)filenameObj) : nil);
	
	jobject filterObj = GetProperty(env, props, @"file_filter");
	NSArray *nsFilters = (filterObj ? GetAllowedFiletypes(env, filterObj) : nil);
	
	jobject messageObj = GetProperty(env, props, @"message");
	NSString *message = (messageObj ? JNFJavaToNSString(env, (jstring)messageObj) : nil);
	
	jobject canCreateFoldersObj = GetProperty(env, props, @"can_create_directories");
	bool canCreateFolders = (canCreateFoldersObj ? GetBool(env, canCreateFoldersObj) : false);
	
	jobject showHiddenObj = GetProperty(env, props, @"show_hidden");
	bool showHidden = (showHiddenObj ? GetBool(env, showHiddenObj) : false);
	
	jobject gListener = env->NewGlobalRef(GetProperty(env, props, @"listener"));
	
    void (^block)(void);
    block = ^(void){
        NSSavePanel *savePanel = [[NSSavePanel savePanel] retain];
        [savePanel setCanCreateDirectories:canCreateFolders];
        [savePanel setShowsHiddenFiles:showHidden];
		
		if(title)
			[savePanel setTitle:title];
			
		if(nsFilters)
			[savePanel setAllowedFileTypes:nsFilters];
			
		if(initialFolder) {
			NSURL *nsStartURL = [NSURL fileURLWithPath:initialFolder];
			[savePanel setDirectoryURL:nsStartURL];
		}
		
		if(filename) {
			[savePanel setNameFieldStringValue:filename];
		}
		
		if(prompt) {
			[savePanel setPrompt:prompt];
		}
		
		if(nameFieldLabel) {
			[savePanel setNameFieldLabel:nameFieldLabel];
		}
		
		if(message) {
			[savePanel setMessage:message];
		}
		
		void (^handler)(NSInteger) = ^(NSInteger result){
			NSURL *selectedURL = nil;
			if (result == NSFileHandlingPanelOKButton) {
		        selectedURL = [savePanel URL];
		    }
		    
		    bool detach = false;
			JNIEnv *env = NULL;
			GetJNIEnv(&env, &detach);
	    	jobject dlgevt = NULL;
		    if(selectedURL != nil) {
		    	jstring filePath = JNFNSToJavaString(env, [selectedURL path]);
		    	dlgevt = createDialogResult(env, RESULT_OK, filePath);
		    } else {
		    	dlgevt = createDialogResult(env, RESULT_CANCEL, NULL);
		    }

			[savePanel orderOut:nil];
			[savePanel release];
		
			sendDialogResult(env, gListener, dlgevt);
			
			env->DeleteGlobalRef(gListener);
		    
		};
		
		if(parentWindow) {
			[savePanel beginSheetModalForWindow:parentWindow completionHandler:handler];
		} else {
			[savePanel beginWithCompletionHandler:handler];
		}
		
    };
    
    if ([NSThread isMainThread]){
        block();
    } else {
        [JNFRunLoop performOnMainThreadWaiting:YES withBlock:block];
    }
    
	JNF_COCOA_EXIT(env)
}

/**
 * NSAlert completion handler
 */
@interface AlertFinished : NSObject
@property (readwrite,assign) bool yesNo;
- (id)init;
- (void)alertDidEnd:(NSAlert*)alert returnCode:(NSInteger)returnCode contextInfo:(void *)contextInfo;
- (void)sendEventForNSAlertReturn:(NSInteger)alertReturn jnienv:(JNIEnv*)env dialogListener:(jobject)listener data:(jobject)data; 
@end

@implementation AlertFinished 

- (id)init {
	id retVal = [super init];
	
	if(retVal) {
		[self setYesNo:NO];
	}
	
	return retVal;
}

- (void)alertDidEnd:(NSAlert*)alert returnCode:(NSInteger)returnCode contextInfo:(void *)contextInfo {
	// context info should be a global reference to the dialog listener
	jobject gListener = (jobject)contextInfo;
	
	// get JNIEnv
	bool detach;
	JNIEnv* env;
	GetJNIEnv(&env, &detach);
	
	jobject data = NULL;
	if([alert showsSuppressionButton]) { 
		data = ToBool(env, [[alert suppressionButton] state] == NSOnState );
	}

	[[alert window] orderOut:nil];	
	[alert release];

	[self sendEventForNSAlertReturn:returnCode jnienv:env dialogListener:gListener data:data];
	
	env->DeleteGlobalRef(gListener);
	
	[self release];
}

- (void)sendEventForNSAlertReturn:(NSInteger)alertReturn jnienv:(JNIEnv*)env dialogListener:(jobject)listener data:(jobject)data {
	int resultCode = alertReturn - NSAlertFirstButtonReturn;
	jobject dlgevt = createDialogResult(env, resultCode, data);
	sendDialogResult(env, listener, dlgevt);
}

@end


/*
 * Class:     ca_phon_ui_nativedialogs_NativeDialogs
 * Method:    nativeShowMessageDialog
 * Signature: (Lca/phon/ui/nativedialogs/MessageDialogProperties;)V
 */
JNIEXPORT void JNICALL Java_ca_phon_ui_nativedialogs_NativeDialogs_nativeShowMessageDialog
  (JNIEnv *env, jclass clazz, jobject props) {
	JNF_COCOA_ENTER(env)
	
	// get all relevant properties
	jobject headerObj = GetProperty(env, props, @"header");
	NSString *header = (headerObj ? JNFJavaToNSString(env, (jstring)headerObj) : nil);
	
	jobject parentWindowObj = GetProperty(env, props, @"parent_window");
	NSWindow *parentWindow = (parentWindowObj ? convertToNSWindow(env, parentWindowObj) : nil);
	
	jobject messageObj = GetProperty(env, props, @"message");
	NSString *message = (messageObj ? JNFJavaToNSString(env, (jstring)messageObj) : nil);
	
	jobject optionsObj = GetProperty(env, props, @"options");
	jobjectArray options = (optionsObj ? (jobjectArray)optionsObj : 
								env->NewObjectArray(1, env->FindClass("java/lang/String"), env->NewStringUTF("Ok")));
	
	jobject showSuppressionObj = GetProperty(env, props, @"show_suppression_box");
	bool showSuppression = (showSuppressionObj ? GetBool(env, showSuppressionObj) : false);
	
	jobject suppressionMessageObj = GetProperty(env, props, @"suppression_message");
	NSString *suppressionMessage = (suppressionMessageObj ? JNFJavaToNSString(env, (jstring)suppressionMessageObj) : nil);
	
	jobject listener = GetProperty(env, props, @"listener");
	jobject gListener = env->NewGlobalRef(listener);
	
	NSMutableArray *nsOptions = [[NSMutableArray alloc] init];
	int count = env->GetArrayLength(options);
	for(int i = 0; i < count; i++) {
		jobject obj = env->GetObjectArrayElement(options, i);
		NSString *btnTxt = JNFJavaToNSString(env, (jstring)obj);
		[nsOptions addObject:btnTxt];
	}
	
	
	AlertFinished *alertListener = 
		[[[AlertFinished alloc] init] retain];
	[alertListener setYesNo:YES];
	// create alert
	void (^block)(void);
	block = ^(void) {
		NSAlert *alert = [[[NSAlert alloc] init] retain];
		[alert setAlertStyle:NSWarningAlertStyle];
		[alert setMessageText:header];
		[alert setInformativeText:message];
		[alert setShowsSuppressionButton:showSuppression];
		if(showSuppression && suppressionMessage) {
			[[alert suppressionButton] setTitle:suppressionMessage];
		}
		
		for(int i = 0; i < [nsOptions count]; i++) {
			[alert addButtonWithTitle:[nsOptions objectAtIndex:i]];
		}
		
		if(parentWindow) {
			[alert 
				beginSheetModalForWindow:parentWindow
				modalDelegate:alertListener
				didEndSelector:@selector(alertDidEnd:returnCode:contextInfo:)
				contextInfo:gListener];
		} else {
			NSInteger alertResult = [alert runModal];
			
			// get JNIEnv
			bool detach;
			JNIEnv* env;
			GetJNIEnv(&env, &detach);
			
			jobject data = NULL;
			if([alert showsSuppressionButton]) { 
				data = ToBool(env, [[alert suppressionButton] state] == NSOnState );
			} else {
				data = ToBool(env, false);
			}

			[[alert window] orderOut:nil];
			[alert release];

			[alertListener sendEventForNSAlertReturn:alertResult jnienv:env dialogListener:gListener data:data];
			
			env->DeleteGlobalRef(gListener);
			[alertListener release];
		}
	};
	
	if ([NSThread isMainThread]){
        block();
    } else {
        [JNFRunLoop performOnMainThreadWaiting:YES withBlock:block];
    }
	
	JNF_COCOA_EXIT(env)
}
