#include <jni.h>
#include <jawt_md.h>
#include <windows.h>
#include <shlobj.h>

#include "jniload.h"
#include "nativedialogs.h"
#include "utils.h"

#include <string>

HWND GetWindowHandle(JNIEnv *env, jobject window) {
	JAWT awt;
	JAWT_DrawingSurface* ds;
	JAWT_DrawingSurfaceInfo* dsi;
	jboolean result;
	jint lock;
	HWND retVal = NULL;

	awt.version = JAWT_VERSION_1_4;

	result = JAWT_GetAWT(env, &awt);
	if (result == JNI_FALSE)
		return NULL;

	// Get the drawing surface
	ds = awt.GetDrawingSurface(env, window);
	if (ds == NULL) return NULL;

	// Lock the drawing surface
	lock = ds->Lock(ds);
	if ((lock & JAWT_LOCK_ERROR) != 0) {
		return NULL;
	}

	// Get the drawing surface info
	dsi = ds->GetDrawingSurfaceInfo(ds);

	JAWT_Win32DrawingSurfaceInfo *dsi_win;
	dsi_win = (JAWT_Win32DrawingSurfaceInfo*)dsi->platformInfo;

	// Get the NSView corresponding to the component that was passed
	retVal = dsi_win->hwnd;

	// Free the drawing surface info
	ds->FreeDrawingSurfaceInfo(dsi);
	// Unlock the drawing surface
	ds->Unlock(ds);

	// Free the drawing surface
	awt.FreeDrawingSurface(ds);

	// Get the view's parent window; this is what we need to show a sheet
	return retVal;
}


std::wstring ToWStr(std::string &str) {
	int size_needed = MultiByteToWideChar(CP_UTF8, 0, &str[0], (int)str.size(), NULL, 0);
	std::wstring wstrTo(size_needed, 0);
	MultiByteToWideChar(CP_UTF8, 0, &str[0], (int)str.size(), &wstrTo[0], size_needed);
	return wstrTo;
}

std::string ToMbStr(std::wstring &wstr) {
	int size_needed = WideCharToMultiByte(CP_UTF8, 0, &wstr[0], (int)wstr.size(), NULL, 0, NULL, NULL);
	std::string strTo(size_needed, 0);
	WideCharToMultiByte(CP_UTF8, 0, &wstr[0], (int)wstr.size(), &strTo[0], size_needed, NULL, NULL);
	return strTo;
}

COMDLG_FILTERSPEC allFilesSpec =
	{ L"All Files", L"*.*" }
;

std::string GetDefaultExtension(JNIEnv* env, jobject filefilter) {
	const char *szClassName = "ca/phon/ui/nativedialogs/FileFilter";
	const char *szGetDefaultExtensionName = "getDefaultExtension";
	const char *szGetDefaultExtensionSig = "()Ljava/lang/String;";

	jclass FileFilter = env->FindClass(szClassName);
	jmethodID getDefaultExtension = env->GetMethodID(FileFilter, szGetDefaultExtensionName, szGetDefaultExtensionSig);

	jboolean isCopy = 0;
	jstring defaultExtensionObj = (jstring)env->CallObjectMethod(filefilter, getDefaultExtension);
	const char *defaultExtension = (defaultExtensionObj ? env->GetStringUTFChars(defaultExtensionObj, &isCopy) : NULL);
	std::string retVal( (defaultExtension ? defaultExtension : "") );
	if(defaultExtension)
		env->ReleaseStringUTFChars(defaultExtensionObj, defaultExtension);
	return retVal;
}

void GetAllowedFiletypes(JNIEnv* env, jobject filefilter, COMDLG_FILTERSPEC *filterSpec) {
	const char *szClassName = "ca/phon/ui/nativedialogs/FileFilter";
	const char *szGetAllExtensionsName = "getAllExtensions";
	const char *szGetAllExtensionsSig = "()Ljava/util/List;";
	const char *szGetDescriptionName = "getDescription";
	const char *szGetDescriptionSig = "()Ljava/lang/String;";

	const char *szListClass = "java/util/List";
	const char *szGetMethod = "get";
	const char *szGetMethodSig = "(I)Ljava/lang/Object;";
	const char *szSizeMethod = "size";
	const char *szSizeMethodSig = "()I";

	jclass FileFilter = env->FindClass(szClassName);
	jmethodID getAllExtensions = env->GetMethodID(FileFilter, szGetAllExtensionsName, szGetAllExtensionsSig);
	jmethodID getDescription = env->GetMethodID(FileFilter, szGetDescriptionName, szGetDescriptionSig);

	jclass List = env->FindClass(szListClass);
	jmethodID get = env->GetMethodID(List, szGetMethod, szGetMethodSig);
	jmethodID size = env->GetMethodID(List, szSizeMethod, szSizeMethodSig);

	jboolean isCopy = 0;
	std::string description;
	std::string extensions;

	jstring desc = (jstring)env->CallObjectMethod(filefilter, getDescription);
	const char *szDesc = env->GetStringUTFChars(desc, &isCopy);
	description.append(szDesc);
	env->ReleaseStringUTFChars(desc, szDesc);

	jobject extensionList = env->CallObjectMethod(filefilter, getAllExtensions);
	int extCount = env->CallIntMethod(extensionList, size);
	for (int i = 0; i < extCount; i++) {
		jstring extension = (jstring)env->CallObjectMethod(extensionList, get, i);
		const char *szExt = env->GetStringUTFChars(extension, &isCopy);

		if (i > 0) extensions.append(";\0");
		extensions.append("*.").append(szExt);

		env->ReleaseStringUTFChars(extension, szExt);
	}
	extensions.append("\0\0");

	std::wstring wDesc = ToWStr(description);
	wchar_t *pszName = (wchar_t*)malloc(sizeof(wchar_t)*(wDesc.size()+1));
	ZeroMemory(pszName, wDesc.size() + 1);
	swprintf(pszName, wDesc.size()+1, L"%s", wDesc.c_str());

	std::wstring wSpec = ToWStr(extensions);
	wchar_t *pszSpec = (wchar_t*)malloc(sizeof(wchar_t)*(wSpec.size()+1));
	ZeroMemory(pszSpec, wDesc.size() + 1);
	swprintf(pszSpec, wDesc.size()+1, L"%s", wSpec.c_str());

	filterSpec->pszName = pszName;
	filterSpec->pszSpec = pszSpec;
}


/*
* Class:     ca_phon_ui_nativedialogs_NativeDialogs
* Method:    nativeShowOpenDialog
* Signature: (Lca/phon/ui/nativedialogs/OpenDialogProperties;)V
*/
JNIEXPORT void JNICALL Java_ca_phon_ui_nativedialogs_NativeDialogs_nativeShowOpenDialog
	(JNIEnv *env, jclass clazz, jobject props) {
	jboolean isCopy = false;

	jobject titleObj = GetProperty(env, props, env->NewStringUTF("title"));
	const char *title = (titleObj ? env->GetStringUTFChars((jstring)titleObj, &isCopy) : NULL);

	jobject promptObj = GetProperty(env, props, env->NewStringUTF("prompt"));
	const char *prompt = (promptObj ? env->GetStringUTFChars((jstring)promptObj, &isCopy) : NULL);

	jobject nflObj = GetProperty(env, props, env->NewStringUTF("name_field_label"));
	const char *nameFieldLabel = (nflObj ? env->GetStringUTFChars((jstring)nflObj, &isCopy) : NULL);

	jobject parentWindowObj = GetProperty(env, props, env->NewStringUTF("parent_window"));
	HWND parentWindow = (parentWindowObj ? GetWindowHandle(env, parentWindowObj) : NULL);

	jobject initialFolderObj = GetProperty(env, props, env->NewStringUTF("initial_folder"));
	const char *initialFolder = (initialFolderObj ? env->GetStringUTFChars((jstring)initialFolderObj, &isCopy) : NULL);
	IShellItem *initialFolderItem = NULL;
	if (initialFolder) {
		PIDLIST_ABSOLUTE pidl;
		SHParseDisplayName(ToWStr(std::string(initialFolder)).c_str(), 0, &pidl, SFGAO_FOLDER, 0);
		SHCreateShellItem(NULL, NULL, pidl, &initialFolderItem);
	}

	jobject filenameObj = GetProperty(env, props, env->NewStringUTF("initial_file"));
	const char *filename = (filenameObj ? env->GetStringUTFChars((jstring)filenameObj, &isCopy) : NULL);

	jobject filterObj = GetProperty(env, props, env->NewStringUTF("file_filter"));

	int totalFilters = 1;
	COMDLG_FILTERSPEC *filters;
	COMDLG_FILTERSPEC customFilter;
	ZeroMemory(&customFilter, sizeof(COMDLG_FILTERSPEC));
	if (filterObj) {
		GetAllowedFiletypes(env, filterObj, &customFilter);

		filters = new COMDLG_FILTERSPEC[2];
		filters[0] = customFilter;
		filters[1] = allFilesSpec;
		++totalFilters;
	}
	else {
		filters = new COMDLG_FILTERSPEC[1];
		filters[0] = allFilesSpec;
	}
	const char *defaultExt = (filterObj ? GetDefaultExtension(env, filterObj).c_str() : NULL);


	jobject messageObj = GetProperty(env, props, env->NewStringUTF("message"));
	const char *message = (messageObj ? env->GetStringUTFChars((jstring)messageObj, &isCopy) : NULL);

	jobject canCreateFoldersObj = GetProperty(env, props, env->NewStringUTF("can_create_directories"));
	bool canCreateFolders = (canCreateFoldersObj ? GetBool(env, canCreateFoldersObj) : false);

	jobject showHiddenObj = GetProperty(env, props, env->NewStringUTF("show_hidden"));
	bool showHidden = (showHiddenObj ? GetBool(env, showHiddenObj) : false);

	jobject canSelectFilesObj = GetProperty(env, props, env->NewStringUTF("can_choose_files"));
	bool canSelectFiles = (canSelectFilesObj ? GetBool(env, canSelectFilesObj) : NULL);

	jobject canSelectFoldersObj = GetProperty(env, props, env->NewStringUTF("can_choose_directories"));
	bool canSelectFolders = (canSelectFoldersObj ? GetBool(env, canSelectFoldersObj) : NULL);

	jobject allowMultipleSelectionObj = GetProperty(env, props, env->NewStringUTF("allow_multiple_selection"));
	bool allowMultipleSelection = (allowMultipleSelectionObj ? GetBool(env, allowMultipleSelectionObj) : NULL);

	jobject gListener = env->NewGlobalRef(GetProperty(env, props, env->NewStringUTF("listener")));

	CoInitialize(NULL);

	IFileOpenDialog *pfd = NULL;
	HRESULT hr = CoCreateInstance(CLSID_FileOpenDialog,
		NULL,
		CLSCTX_INPROC_SERVER,
		IID_PPV_ARGS(&pfd));
	if (SUCCEEDED(hr)) {
		if (title)
			pfd->SetTitle(ToWStr(std::string(title)).c_str());
		if (prompt)
			pfd->SetOkButtonLabel(ToWStr(std::string(prompt)).c_str());
		if (nameFieldLabel)
			pfd->SetFileNameLabel(ToWStr(std::string(nameFieldLabel)).c_str());
		if (filename)
			pfd->SetFileName(ToWStr(std::string(filename)).c_str());
		pfd->SetFileTypes(totalFilters, filters);
		if(defaultExt)
			pfd->SetDefaultExtension(ToWStr(std::string(defaultExt)).c_str());

		if (initialFolderItem)
			pfd->SetDefaultFolder(initialFolderItem);

		FILEOPENDIALOGOPTIONS opts;
		pfd->GetOptions(&opts);
		opts |= FOS_FILEMUSTEXIST;
		opts |= FOS_PATHMUSTEXIST;
		if (showHidden) {
			opts |= FOS_FORCESHOWHIDDEN;
		}
		if (canSelectFolders) {
			opts |= FOS_PICKFOLDERS;
		}
		if (allowMultipleSelection) {
			opts |= FOS_ALLOWMULTISELECT;
		}
		pfd->SetOptions(opts);
		
		hr = pfd->Show(parentWindow);
		if (SUCCEEDED(hr)) {
			jobject data = NULL;

			if (allowMultipleSelection) {
				IShellItemArray *psiaResults;
				hr = pfd->GetResults(&psiaResults);

				if (SUCCEEDED(hr)) {
					DWORD numItems;
					psiaResults->GetCount(&numItems);

					jclass String = env->FindClass("java/lang/String");
					data = env->NewObjectArray(numItems, String, 0);

					for (int i = 0; i < numItems; i++) {
						IShellItem *psiResult;
						hr = psiaResults->GetItemAt(i, &psiResult);

						if (SUCCEEDED(hr)) {
							LPWSTR pszDisplayName = NULL;
							psiResult->GetDisplayName(SIGDN_FILESYSPATH, &pszDisplayName);

							jstring jstr = env->NewStringUTF(ToMbStr(std::wstring(pszDisplayName)).c_str());
							env->SetObjectArrayElement((jobjectArray)data, i, jstr);

							psiResult->Release();
						}

					}

					psiaResults->Release();
				}
			}
			else {
				IShellItem *psiResult;
				hr = pfd->GetResult(&psiResult);

				if (SUCCEEDED(hr)) {
					LPWSTR pszDisplayName;
					psiResult->GetDisplayName(SIGDN_FILESYSPATH, &pszDisplayName);

					data = env->NewStringUTF(ToMbStr(std::wstring(pszDisplayName)).c_str());

					psiResult->Release();
				}
			}

			// send event
			jobject dlgevt = NULL;
			if (data != NULL) {
				dlgevt = CreateDialogResult(env, RESULT_OK, data);
			}
			else {
				dlgevt = CreateDialogResult(env, RESULT_CANCEL, NULL);
			}

			SendDialogResult(env, gListener, dlgevt);
		}
		else {
			jobject dlgevt = CreateDialogResult(env, RESULT_CANCEL, NULL);
			SendDialogResult(env, gListener, dlgevt);
		}
		pfd->Release();
	}
	else {
		jobject dlgevt = CreateDialogResult(env, RESULT_CANCEL, NULL);
		SendDialogResult(env, gListener, dlgevt);
	}

	if(title)
		env->ReleaseStringUTFChars((jstring)titleObj, title);
	if(prompt)
		env->ReleaseStringUTFChars((jstring)promptObj, prompt);
	if (nameFieldLabel)
		env->ReleaseStringUTFChars((jstring)nflObj, nameFieldLabel);
	if (initialFolder)
		env->ReleaseStringUTFChars((jstring)initialFolderObj, initialFolder);
	if (filename)
		env->ReleaseStringUTFChars((jstring)filenameObj, filename);
	if (message)
		env->ReleaseStringUTFChars((jstring)messageObj, message);
	if(gListener)
		env->DeleteGlobalRef(gListener);
	if (customFilter.pszName)
		delete customFilter.pszName;
	if (customFilter.pszSpec)
		delete customFilter.pszSpec;
	if (initialFolderItem)
		initialFolderItem->Release();
	delete[] filters;
	CoUninitialize();
}

/*
* Class:     ca_phon_ui_nativedialogs_NativeDialogs
* Method:    nativeShowSaveDialog
* Signature: (Lca/phon/ui/nativedialogs/SaveDialogProperties;)V
*/
JNIEXPORT void JNICALL Java_ca_phon_ui_nativedialogs_NativeDialogs_nativeShowSaveDialog
	(JNIEnv *env, jclass clazz, jobject props) {
	jboolean isCopy = false;

	jobject titleObj = GetProperty(env, props, env->NewStringUTF("title"));
	const char *title = (titleObj ? env->GetStringUTFChars((jstring)titleObj, &isCopy) : NULL);

	jobject promptObj = GetProperty(env, props, env->NewStringUTF("prompt"));
	const char *prompt = (promptObj ? env->GetStringUTFChars((jstring)promptObj, &isCopy) : NULL);

	jobject nflObj = GetProperty(env, props, env->NewStringUTF("name_field_label"));
	const char *nameFieldLabel = (nflObj ? env->GetStringUTFChars((jstring)nflObj, &isCopy) : NULL);

	jobject parentWindowObj = GetProperty(env, props, env->NewStringUTF("parent_window"));
	HWND parentWindow = (parentWindowObj ? GetWindowHandle(env, parentWindowObj) : NULL);

	jobject initialFolderObj = GetProperty(env, props, env->NewStringUTF("initial_folder"));
	const char *initialFolder = (initialFolderObj ? env->GetStringUTFChars((jstring)initialFolderObj, &isCopy) : NULL);
	IShellItem *initialFolderItem = NULL;
	if (initialFolder) {
		PIDLIST_ABSOLUTE pidl;
		SHParseDisplayName(ToWStr(std::string(initialFolder)).c_str(), 0, &pidl, SFGAO_FOLDER, 0);
		SHCreateShellItem(NULL, NULL, pidl, &initialFolderItem);
	}

	jobject filenameObj = GetProperty(env, props, env->NewStringUTF("initial_file"));
	const char *filename = (filenameObj ? env->GetStringUTFChars((jstring)filenameObj, &isCopy) : NULL);

	jobject filterObj = GetProperty(env, props, env->NewStringUTF("file_filter"));
	int totalFilters = 1;
	COMDLG_FILTERSPEC *filters;
	COMDLG_FILTERSPEC customFilter;
	ZeroMemory(&customFilter, sizeof(COMDLG_FILTERSPEC));
	if (filterObj) {
		GetAllowedFiletypes(env, filterObj, &customFilter);

		filters = new COMDLG_FILTERSPEC[2];
		filters[0] = customFilter;
		filters[1] = allFilesSpec;
		++totalFilters;
	}
	else {
		filters = new COMDLG_FILTERSPEC[1];
		filters[0] = allFilesSpec;
	}

	const char *defaultExt = (filterObj ? GetDefaultExtension(env, filterObj).c_str() : NULL);

	jobject messageObj = GetProperty(env, props, env->NewStringUTF("message"));
	const char *message = (messageObj ? env->GetStringUTFChars((jstring)messageObj, &isCopy) : NULL);

	jobject canCreateFoldersObj = GetProperty(env, props, env->NewStringUTF("can_create_directories"));
	bool canCreateFolders = (canCreateFoldersObj ? GetBool(env, canCreateFoldersObj) : false);

	jobject showHiddenObj = GetProperty(env, props, env->NewStringUTF("show_hidden"));
	bool showHidden = (showHiddenObj ? GetBool(env, showHiddenObj) : false);

	jobject gListener = env->NewGlobalRef(GetProperty(env, props, env->NewStringUTF("listener")));

	CoInitialize(NULL);

	IFileSaveDialog *pfd = NULL;
	HRESULT hr = CoCreateInstance(CLSID_FileSaveDialog,
		NULL,
		CLSCTX_INPROC_SERVER,
		IID_PPV_ARGS(&pfd));
	if (SUCCEEDED(hr)) {
		if (title)
			pfd->SetTitle(ToWStr(std::string(title)).c_str());
		if (prompt)
			pfd->SetOkButtonLabel(ToWStr(std::string(prompt)).c_str());
		if (nameFieldLabel)
			pfd->SetFileNameLabel(ToWStr(std::string(nameFieldLabel)).c_str());
		if (filename)
			pfd->SetFileName(ToWStr(std::string(filename)).c_str());
		pfd->SetFileTypes(totalFilters, filters);
		if (defaultExt)
			pfd->SetDefaultExtension(ToWStr(std::string(defaultExt)).c_str());

		if (initialFolderItem)
			pfd->SetDefaultFolder(initialFolderItem);

		FILEOPENDIALOGOPTIONS opts;
		pfd->GetOptions(&opts);
		opts |= FOS_OVERWRITEPROMPT;
		opts |= FOS_PATHMUSTEXIST;
		if (showHidden) {
			opts |= FOS_FORCESHOWHIDDEN;
		}
		pfd->SetOptions(opts);

		hr = pfd->Show(parentWindow);
		if (SUCCEEDED(hr)) {
			IShellItem *psiResult;
			hr = pfd->GetResult(&psiResult);
			jobject data = NULL;

			if (SUCCEEDED(hr)) {
				LPWSTR pszDisplayName;
				psiResult->GetDisplayName(SIGDN_FILESYSPATH, &pszDisplayName);

				data = env->NewStringUTF(ToMbStr(std::wstring(pszDisplayName)).c_str());
				
				psiResult->Release();
			}

			// send event
			jobject dlgevt = NULL;
			if (data != NULL) {
				dlgevt = CreateDialogResult(env, RESULT_OK, data);
			}
			else {
				dlgevt = CreateDialogResult(env, RESULT_CANCEL, NULL);
			}

			SendDialogResult(env, gListener, dlgevt);
		}
		else {
			jobject dlgevt = CreateDialogResult(env, RESULT_CANCEL, NULL);
			SendDialogResult(env, gListener, dlgevt);
		}


		pfd->Release();
	}
	else {
		jobject dlgevt = CreateDialogResult(env, RESULT_CANCEL, NULL);
		SendDialogResult(env, gListener, dlgevt);
	}

	if (title)
		env->ReleaseStringUTFChars((jstring)titleObj, title);
	if (prompt)
		env->ReleaseStringUTFChars((jstring)promptObj, prompt);
	if (nameFieldLabel)
		env->ReleaseStringUTFChars((jstring)nflObj, nameFieldLabel);
	if (initialFolder)
		env->ReleaseStringUTFChars((jstring)initialFolderObj, initialFolder);
	if (initialFolderItem)
		initialFolderItem->Release();
	if (filename)
		env->ReleaseStringUTFChars((jstring)filenameObj, filename);
	if (message)
		env->ReleaseStringUTFChars((jstring)messageObj, message);
	if (gListener)
		env->DeleteGlobalRef(gListener);
	if (customFilter.pszName)
		delete customFilter.pszName;
	if (customFilter.pszSpec)
		delete customFilter.pszSpec;
	delete[] filters;
	CoUninitialize();
}
