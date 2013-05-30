

// cocoa includes
#import <Foundation/Foundation.h>
#import <Cocoa/Cocoa.h>

#import <JavaVM/jni.h>
#import <JavaVM/jawt.h>
#import <JavaVM/jawt_md.h>
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
	JAWT awt;
	JAWT_DrawingSurface* ds;
	JAWT_DrawingSurfaceInfo* dsi;
	JAWT_MacOSXDrawingSurfaceInfo* dsi_mac;
	jboolean result;
	jint lock;
    
	// Get the AWT
	awt.version = JAWT_VERSION_1_4;
	result = JAWT_GetAWT(env, &awt);
	assert(result != JNI_FALSE);
    
	// Get the drawing surface
	ds = awt.GetDrawingSurface(env, window);
	assert(ds != NULL);
    
	// Lock the drawing surface
	lock = ds->Lock(ds);
	assert((lock & JAWT_LOCK_ERROR) == 0);
    
	// Get the drawing surface info
	dsi = ds->GetDrawingSurfaceInfo(ds);
    
	// Get the platform-specific drawing info
	dsi_mac = (JAWT_MacOSXDrawingSurfaceInfo*)dsi->platformInfo;
    
	// Get the NSView corresponding to the component that was passed
	NSView *view = dsi_mac->cocoaViewRef;

	// Free the drawing surface info
	ds->FreeDrawingSurfaceInfo(dsi);
	// Unlock the drawing surface
	ds->Unlock(ds);
    
	// Free the drawing surface
	awt.FreeDrawingSurface(ds);

	// Get the view's parent window; this is what we need to show a sheet
	return [view window];
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
NSArray *processFilters(JNIEnv *env, jobjectArray filters) {
	const char *szClassName = "ca/phon/ui/nativedialogs/FileFilter";
	const char *szMethodSig = "()[Ljava/lang/String;";
	const char *szMethodName = "exts";
	
	NSMutableArray *retVal = [[NSMutableArray alloc] init];
	
	jclass FileFilter = env->FindClass(szClassName);
	jmethodID exts = env->GetMethodID(FileFilter, szMethodName, szMethodSig);
	
	int count = env->GetArrayLength(filters);
	for(int i = 0; i < count; i++) {
		jobject filter = env->GetObjectArrayElement(filters, i);
		jobjectArray extArray = (jobjectArray)env->CallObjectMethod(filter, exts);
		
		// add each extension to our return array
		int extCount = env->GetArrayLength(extArray);
		for(int j = 0; j < extCount; j++) {
			jstring ext = (jstring)env->GetObjectArrayElement(extArray, j);
			[retVal addObject:[JNFJavaToNSString(env, ext) substringFromIndex:1]];
		}
	}
	
	return retVal;
}

/*
 * Class:     ca_phon_ui_nativedialogs_NativeDialogs
 * Method:    nativeBrowseForFile
 * Signature: (Ljava/awt/Window;Lca/phon/ui/nativedialogs/NativeDialogListener;Ljava/lang/String;Ljava/lang/String;[Lca/phon/ui/nativedialogs/FileFilter;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ca_phon_ui_nativedialogs_NativeDialogs_nativeBrowseForFile
  (JNIEnv *env, jclass clazz, jobject parentWindow, 
	jobject listener, jstring startDir, jstring defaultExt, 
	jobjectArray filters, jstring title) {
	JNF_COCOA_ENTER(env)
	
	// convert window ref
	NSWindow *nsParentWindow = 
		(parentWindow != NULL ? convertToNSWindow(env, parentWindow) : NULL);

	// convert strings
	NSString *nsStartFolder = 
		(startDir != NULL ? JNFJavaToNSString(env, startDir) : NULL);
	NSString *nsDefaultExt = 
		(defaultExt != NULL ? JNFJavaToNSString(env, defaultExt) : NULL);
	NSString *nsTitle =
		(title != NULL ? JNFJavaToNSString(env, title) : NULL);
		
	NSArray *nsFilters = 
		(filters != NULL ? processFilters(env, filters) : NULL);
	
	if(nsFilters == NULL) {
		// check for a default extension
		nsFilters = [NSArray arrayWithObject:nsDefaultExt];
	}
		
	jobject gListener = env->NewGlobalRef(listener);
	
    void (^block)(void);
    block = ^(void){
        NSOpenPanel *openPanel = [[NSOpenPanel openPanel] retain];
		[openPanel setCanChooseFiles:YES];
		[openPanel setCanChooseDirectories:NO];
		[openPanel setAllowsMultipleSelection:NO];
		
		if(nsTitle)
			[openPanel setTitle:nsTitle];
			
		if(nsFilters)
			[openPanel setAllowedFileTypes:nsFilters];
			
		if(nsStartFolder) {
			NSURL *nsStartURL = [NSURL URLWithString:nsStartFolder];
			[openPanel setDirectoryURL:nsStartURL];
		}
		
		void (^handler)(NSInteger) = ^(NSInteger result){
			NSURL *selectedURL = nil;
			if (result == NSFileHandlingPanelOKButton) {
		        for (NSURL *fileURL in [openPanel URLs]) {
		        	selectedURL = fileURL;
		        }
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
		
			sendDialogResult(env, gListener, dlgevt);
			
			env->DeleteGlobalRef(gListener);
		    
			[openPanel release];
		};
		
		if(nsParentWindow) {
			[openPanel beginSheetModalForWindow:nsParentWindow completionHandler:handler];
		} else {
			[openPanel beginWithCompletionHandler:handler];
		}
		
    };
    
    if ( [NSThread isMainThread]){
        block();
    } else {
        [JNFRunLoop performOnMainThreadWaiting:YES withBlock:block];
    }
    
	JNF_COCOA_EXIT(env)
}

/*
 * Class:     ca_phon_ui_nativedialogs_NativeDialogs
 * Method:    nativeBrowseForDirectory
 * Signature: (Ljava/awt/Window;Lca/phon/ui/nativedialogs/NativeDialogListener;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ca_phon_ui_nativedialogs_NativeDialogs_nativeBrowseForDirectory
  (JNIEnv *env, jclass clazz, jobject parentWindow, jobject listener, jstring startDir, jstring title) {
	JNF_COCOA_ENTER(env)
	
	// convert window ref
	NSWindow *nsParentWindow = 
		(parentWindow != NULL ? convertToNSWindow(env, parentWindow) : NULL);

	// convert strings
	NSString *nsStartFolder = 
		(startDir != NULL ? JNFJavaToNSString(env, startDir) : NULL);
	NSString *nsTitle =
		(title != NULL ? JNFJavaToNSString(env, title) : NULL);
		
	jobject gListener = env->NewGlobalRef(listener);
	
    void (^block)(void);
    block = ^(void){
        NSOpenPanel *openPanel = [[NSOpenPanel openPanel] retain];
		[openPanel setCanChooseFiles:NO];
		[openPanel setCanChooseDirectories:YES];
		[openPanel setAllowsMultipleSelection:NO];
		[openPanel setCanCreateDirectories:YES];
		
		if(nsTitle)
			[openPanel setTitle:nsTitle];
			
		if(nsStartFolder) {
			NSURL *nsStartURL = [NSURL URLWithString:nsStartFolder];
			[openPanel setDirectoryURL:nsStartURL];
		}
		
		void (^handler)(NSInteger) = ^(NSInteger result){
			NSURL *selectedURL = nil;
			if (result == NSFileHandlingPanelOKButton) {
		        for (NSURL *fileURL in [openPanel URLs]) {
		        	selectedURL = fileURL;
		        }
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
		
			sendDialogResult(env, gListener, dlgevt);
			
			env->DeleteGlobalRef(gListener);
		    
			[openPanel release];
		};
		
		if(nsParentWindow) {
			[openPanel beginSheetModalForWindow:nsParentWindow completionHandler:handler];
		} else {
			[openPanel beginWithCompletionHandler:handler];
		}
		
    };
    
    if ( [NSThread isMainThread]){
        block();
    } else {
        [JNFRunLoop performOnMainThreadWaiting:YES withBlock:block];
    }
    
	JNF_COCOA_EXIT(env)
}

/*
 * Class:     ca_phon_ui_nativedialogs_NativeDialogs
 * Method:    nativeShowSaveFileDialog
 * Signature: (Ljava/awt/Window;Lca/phon/ui/nativedialogs/NativeDialogListener;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Lca/phon/ui/nativedialogs/FileFilter;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ca_phon_ui_nativedialogs_NativeDialogs_nativeShowSaveFileDialog
  (JNIEnv *env, jclass clazz, jobject parentWindow, jobject listener, 
	jstring startDir, jstring fileName, jstring defaultExt, 
	jobjectArray filters, jstring title) {
	JNF_COCOA_ENTER(env)
	
	// convert window ref
	NSWindow *nsParentWindow = 
		(parentWindow != NULL ? convertToNSWindow(env, parentWindow) : NULL);

	// convert strings
	NSString *nsStartFolder = 
		(startDir != NULL ? JNFJavaToNSString(env, startDir) : NULL);
	NSString *nsDefaultExt = 
		(defaultExt != NULL ? JNFJavaToNSString(env, defaultExt) : NULL);
	NSString *nsTitle =
		(title != NULL ? JNFJavaToNSString(env, title) : NULL);
	NSString *nsFile =
		(fileName != NULL ? JNFJavaToNSString(env, fileName) : NULL);
		
	NSArray *nsFilters = 
		(filters != NULL ? processFilters(env, filters) : NULL);
	
	if(nsFilters == NULL) {
		// check for a default extension
		nsFilters = [NSArray arrayWithObject:nsDefaultExt];
	}
		
	jobject gListener = env->NewGlobalRef(listener);
	
    void (^block)(void);
    block = ^(void){
        NSSavePanel *savePanel = [[NSSavePanel savePanel] retain];
        [savePanel setCanCreateDirectories:YES];
		
		if(nsTitle)
			[savePanel setTitle:nsTitle];
			
		if(nsFilters)
			[savePanel setAllowedFileTypes:nsFilters];
			
		if(nsStartFolder) {
			NSURL *nsStartURL = [NSURL URLWithString:nsStartFolder];
			[savePanel setDirectoryURL:nsStartURL];
		}
		
		if(nsFile) {
			[savePanel setNameFieldStringValue:nsFile];
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
		
			sendDialogResult(env, gListener, dlgevt);
			
			env->DeleteGlobalRef(gListener);
		    
			[savePanel release];
		};
		
		if(nsParentWindow) {
			[savePanel beginSheetModalForWindow:nsParentWindow completionHandler:handler];
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
- (void)sendEventForNSAlertReturn:(NSInteger)alertReturn jnienv:(JNIEnv*)env dialogListener:(jobject)listener; 
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
	
	[self sendEventForNSAlertReturn:returnCode jnienv:env dialogListener:gListener];
	
	env->DeleteGlobalRef(gListener);
	[alert release];
	[self release];
}

- (void)sendEventForNSAlertReturn:(NSInteger)alertReturn jnienv:(JNIEnv*)env dialogListener:(jobject)listener {
	int resultCode = RESULT_UNKNOWN;
	
	switch(alertReturn) {
	case NSAlertDefaultReturn:
		resultCode = (self.yesNo ? ANSWER_YES : RESULT_OK);
		break;
		
	case NSAlertAlternateReturn:
		resultCode = RESULT_CANCEL;
		break;
		
	case NSAlertOtherReturn:
		resultCode = (self.yesNo ? ANSWER_NO : RESULT_CANCEL);
		break;
		
	default:
		break;
	}
	
	jobject dlgevt = createDialogResult(env, resultCode, NULL);
	sendDialogResult(env, listener, dlgevt);
}

@end


/*
 * Class:     ca_phon_ui_nativedialogs_NativeDialogs
 * Method:    nativeShowYesNoCancelDialog
 * Signature: (Ljava/awt/Window;Lca/phon/ui/nativedialogs/NativeDialogListener;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ca_phon_ui_nativedialogs_NativeDialogs_nativeShowYesNoCancelDialog
  (JNIEnv *env, jclass clazz, jobject parentWindow, jobject listener, jstring unused, jstring title, jstring message) {
	JNF_COCOA_ENTER(env)
	
	// convert window ref
	NSWindow *nsParentWindow = 
		(parentWindow != NULL ? convertToNSWindow(env, parentWindow) : NULL);

	// convert strings
	NSString *nsTitle =
		(title != NULL ? JNFJavaToNSString(env, title) : NULL);
	NSString *nsMsg = 
		(message != NULL ? JNFJavaToNSString(env, message) : NULL);
		
	jobject gListener = env->NewGlobalRef(listener);
	
	AlertFinished *alertListener = 
		[[[AlertFinished alloc] init] retain];
	[alertListener setYesNo:YES];
	// create alert
	void (^block)(void);
	block = ^(void) {
		NSAlert *alert = 
			[[NSAlert 
				alertWithMessageText:nsTitle
				defaultButton: @"Yes"
				alternateButton:@"Cancel"
				otherButton:@"No"
				informativeTextWithFormat:nsMsg] retain];
		
		if(nsParentWindow) {
			[alert 
				beginSheetModalForWindow:nsParentWindow
				modalDelegate:alertListener
				didEndSelector:@selector(alertDidEnd:returnCode:contextInfo:)
				contextInfo:gListener];
		} else {
			NSInteger alertResult = [alert runModal];
			
			// get JNIEnv
			bool detach;
			JNIEnv* env;
			GetJNIEnv(&env, &detach);
			[alertListener sendEventForNSAlertReturn:alertResult jnienv:env dialogListener:gListener];
			
			env->DeleteGlobalRef(gListener);
			[alert release];
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
