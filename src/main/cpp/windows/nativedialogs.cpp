/*
 * Copyright (C) 2012-2018 Gregory Hedlund
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *    http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <jni.h>
#include <jawt_md.h>
#include <windows.h>
#include <shlobj.h>
#include <Shlwapi.h>
#include <shobjidl.h>

#include "../jniload.h"
#include "ca_phon_ui_nativedialogs_NativeDialogs.h"
#include "../utils.h"

#include <string>

class CDialogEventHandler : public IFileDialogEvents,
                            public IFileDialogControlEvents
{
public:
    // IUnknown methods
    IFACEMETHODIMP QueryInterface(REFIID riid, void** ppv)
    {
        static const QITAB qit[] = {
                QITABENT(CDialogEventHandler, IFileDialogEvents),
                QITABENT(CDialogEventHandler, IFileDialogControlEvents),
                { 0 },
#pragma warning(suppress:4838)
        };
        return QISearch(this, qit, riid, ppv);
    }

    IFACEMETHODIMP_(ULONG) AddRef()
    {
        return InterlockedIncrement(&_cRef);
    }

    IFACEMETHODIMP_(ULONG) Release()
    {
        long cRef = InterlockedDecrement(&_cRef);
        if (!cRef)
            delete this;
        return cRef;
    }

    // IFileDialogEvents methods
    IFACEMETHODIMP OnFileOk(IFileDialog *) { return S_OK; };
    IFACEMETHODIMP OnFolderChange(IFileDialog *) { return S_OK; };
    IFACEMETHODIMP OnFolderChanging(IFileDialog *, IShellItem *) { return S_OK; };
    IFACEMETHODIMP OnHelp(IFileDialog *) { return S_OK; };
    IFACEMETHODIMP OnSelectionChange(IFileDialog *) { return S_OK; };
    IFACEMETHODIMP OnShareViolation(IFileDialog *, IShellItem *, FDE_SHAREVIOLATION_RESPONSE *) { return S_OK; };
    IFACEMETHODIMP OnTypeChange(IFileDialog *pfd);
    IFACEMETHODIMP OnOverwrite(IFileDialog *, IShellItem *, FDE_OVERWRITE_RESPONSE *) { return S_OK; };

    // IFileDialogControlEvents methods
    IFACEMETHODIMP OnItemSelected(IFileDialogCustomize *pfdc, DWORD dwIDCtl, DWORD dwIDItem) { return S_OK; };
    IFACEMETHODIMP OnButtonClicked(IFileDialogCustomize *, DWORD) { return S_OK; };
    IFACEMETHODIMP OnCheckButtonToggled(IFileDialogCustomize *, DWORD, BOOL) { return S_OK; };
    IFACEMETHODIMP OnControlActivating(IFileDialogCustomize *, DWORD) { return S_OK; };

    CDialogEventHandler(IShellItem *psi) : _cRef(1), _psi(psi) { };
private:
    ~CDialogEventHandler() { };
    IShellItem *_psi;
    long _cRef;
};

HRESULT CDialogEventHandler::OnTypeChange(IFileDialog *pfd)
{
    BOOL wasGUIThread = IsGUIThread(true);
    HRESULT retVal = pfd->SetDefaultFolder(this->_psi);
    return retVal;
}

HRESULT CDialogEventHandler_CreateInstance(IShellItem *psi, REFIID riid, void **ppv)
{
    *ppv = NULL;
    CDialogEventHandler *pDialogEventHandler = new (std::nothrow) CDialogEventHandler(psi);
    HRESULT hr = pDialogEventHandler ? S_OK : E_OUTOFMEMORY;
    if (SUCCEEDED(hr))
    {
        hr = pDialogEventHandler->QueryInterface(riid, ppv);
        pDialogEventHandler->Release();
    }
    return hr;
}

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
		awt.FreeDrawingSurface(ds);
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


const std::wstring ToWStr(const std::string &str) {
	int size_needed = MultiByteToWideChar(CP_UTF8, 0, &str[0], (int)str.size(), NULL, 0);
	std::wstring wstrTo(size_needed, 0);
	MultiByteToWideChar(CP_UTF8, 0, &str[0], (int)str.size(), &wstrTo[0], size_needed);
	return wstrTo;
}

const std::string ToMbStr(const std::wstring &wstr) {
	int size_needed = WideCharToMultiByte(CP_UTF8, 0, &wstr[0], (int)wstr.size(), NULL, 0, NULL, NULL);
	std::string strTo(size_needed, 0);
	WideCharToMultiByte(CP_UTF8, 0, &wstr[0], (int)wstr.size(), &strTo[0], size_needed, NULL, NULL);
	return strTo;
}

COMDLG_FILTERSPEC allFilesSpec =
	{ L"All Files", L"*.*" }
;

const std::string GetDefaultExtension(JNIEnv* env, jobject filefilter) {
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

	std::wstring wDesc = ToWStr(description);
	wchar_t *pszName = (wchar_t*)malloc(sizeof(wchar_t)*wDesc.size()+1);
	memset( pszName, L'\0', wDesc.size() + 1 );
	swprintf(pszName, wDesc.size(), L"%s\0", wDesc.c_str() );

	std::wstring wSpec = ToWStr(extensions);
	wchar_t *pszSpec = (wchar_t*)malloc(sizeof(wchar_t)*(wSpec.size()+2));
	wmemset( pszSpec, L'\0', wSpec.size() + 2);
	wmemcpy( pszSpec, wSpec.c_str(), wSpec.size());

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

	jobject titleObj = GetProperty(env, props, env->NewStringUTF(u8"title"));
	const char *title = (titleObj ? env->GetStringUTFChars((jstring)titleObj, &isCopy) : NULL);

	jobject promptObj = GetProperty(env, props, env->NewStringUTF(u8"prompt"));
	const char *prompt = (promptObj ? env->GetStringUTFChars((jstring)promptObj, &isCopy) : NULL);

	jobject nflObj = GetProperty(env, props, env->NewStringUTF(u8"name_field_label"));
	const char *nameFieldLabel = (nflObj ? env->GetStringUTFChars((jstring)nflObj, &isCopy) : NULL);

	jobject parentWindowObj = GetProperty(env, props, env->NewStringUTF(u8"parent_window"));
	HWND parentWindow = (parentWindowObj ? GetWindowHandle(env, parentWindowObj) : NULL);

	jobject initialFolderObj = GetProperty(env, props, env->NewStringUTF(u8"initial_folder"));
	const char *initialFolder = (initialFolderObj ? env->GetStringUTFChars((jstring)initialFolderObj, &isCopy) : NULL);
	IShellItem *initialFolderItem = NULL;
	if (initialFolder) {
		const std::wstring wInitialFolder = ToWStr(std::string(initialFolder));
		SHCreateItemFromParsingName(wInitialFolder.c_str(), NULL, IID_PPV_ARGS(&initialFolderItem));
	}

	jobject filenameObj = GetProperty(env, props, env->NewStringUTF(u8"initial_file"));
	const char *filename = (filenameObj ? env->GetStringUTFChars((jstring)filenameObj, &isCopy) : NULL);

	jobject filterObj = GetProperty(env, props, env->NewStringUTF(u8"file_filter"));

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
	std::string defaultExtStr = (filterObj ? GetDefaultExtension(env, filterObj) : "");
	const char *defaultExt = (!defaultExtStr.empty() ? defaultExtStr.c_str() : NULL);

	jobject messageObj = GetProperty(env, props, env->NewStringUTF(u8"message"));
	const char *message = (messageObj ? env->GetStringUTFChars((jstring)messageObj, &isCopy) : NULL);

	jobject canCreateFoldersObj = GetProperty(env, props, env->NewStringUTF(u8"can_create_directories"));
	bool canCreateFolders = (canCreateFoldersObj ? GetBool(env, canCreateFoldersObj) : false);

	jobject showHiddenObj = GetProperty(env, props, env->NewStringUTF(u8"show_hidden"));
	bool showHidden = (showHiddenObj ? GetBool(env, showHiddenObj) : false);

	jobject canSelectFilesObj = GetProperty(env, props, env->NewStringUTF(u8"can_choose_files"));
	bool canSelectFiles = (canSelectFilesObj ? GetBool(env, canSelectFilesObj) : false);

	jobject canSelectFoldersObj = GetProperty(env, props, env->NewStringUTF(u8"can_choose_directories"));
	bool canSelectFolders = (canSelectFoldersObj ? GetBool(env, canSelectFoldersObj) : false);

	jobject allowMultipleSelectionObj = GetProperty(env, props, env->NewStringUTF(u8"allow_multiple_selection"));
	bool allowMultipleSelection = (allowMultipleSelectionObj ? GetBool(env, allowMultipleSelectionObj) : false);

	jobject gListener = env->NewGlobalRef(GetProperty(env, props, env->NewStringUTF(u8"listener")));

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

							CoTaskMemFree(pszDisplayName);
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
					LPWSTR pszDisplayName = NULL;
					psiResult->GetDisplayName(SIGDN_FILESYSPATH, &pszDisplayName);

					data = env->NewStringUTF(ToMbStr(std::wstring(pszDisplayName)).c_str());

					CoTaskMemFree(pszDisplayName);
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
		free((void*)customFilter.pszName);
	if (customFilter.pszSpec)
		free((void*)customFilter.pszSpec);
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

	jobject titleObj = GetProperty(env, props, env->NewStringUTF(u8"title"));
	const char *title = (titleObj ? env->GetStringUTFChars((jstring)titleObj, &isCopy) : NULL);

	jobject promptObj = GetProperty(env, props, env->NewStringUTF(u8"prompt"));
	const char *prompt = (promptObj ? env->GetStringUTFChars((jstring)promptObj, &isCopy) : NULL);

	jobject nflObj = GetProperty(env, props, env->NewStringUTF(u8"name_field_label"));
	const char *nameFieldLabel = (nflObj ? env->GetStringUTFChars((jstring)nflObj, &isCopy) : NULL);

	jobject parentWindowObj = GetProperty(env, props, env->NewStringUTF(u8"parent_window"));
	HWND parentWindow = (parentWindowObj ? GetWindowHandle(env, parentWindowObj) : NULL);

	jobject initialFolderObj = GetProperty(env, props, env->NewStringUTF(u8"initial_folder"));
	const char *initialFolder = (initialFolderObj ? env->GetStringUTFChars((jstring)initialFolderObj, &isCopy) : NULL);
	IShellItem *initialFolderItem = NULL;
	if (initialFolder) {
		const std::wstring wInitialFolder = ToWStr(std::string(initialFolder));
		SHCreateItemFromParsingName(wInitialFolder.c_str(), NULL, IID_PPV_ARGS(&initialFolderItem));
	}

	jobject filenameObj = GetProperty(env, props, env->NewStringUTF(u8"initial_file"));
	const char *filename = (filenameObj ? env->GetStringUTFChars((jstring)filenameObj, &isCopy) : NULL);

	jobject filterObj = GetProperty(env, props, env->NewStringUTF(u8"file_filter"));
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

	std::string defaultExtStr = (filterObj ? GetDefaultExtension(env, filterObj) : "");
	const char *defaultExt = (!defaultExtStr.empty() ? defaultExtStr.c_str() : NULL);

	jobject messageObj = GetProperty(env, props, env->NewStringUTF(u8"message"));
	const char *message = (messageObj ? env->GetStringUTFChars((jstring)messageObj, &isCopy) : NULL);

	jobject canCreateFoldersObj = GetProperty(env, props, env->NewStringUTF(u8"can_create_directories"));
	bool canCreateFolders = (canCreateFoldersObj ? GetBool(env, canCreateFoldersObj) : false);

	jobject showHiddenObj = GetProperty(env, props, env->NewStringUTF(u8"show_hidden"));
	bool showHidden = (showHiddenObj ? GetBool(env, showHiddenObj) : false);

	jobject gListener = env->NewGlobalRef(GetProperty(env, props, env->NewStringUTF(u8"listener")));

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

        DWORD dlgEventsCookie = 0L;
        IFileDialogEvents *pfde = NULL;
		if (initialFolderItem) {
            BOOL guiThread = IsGUIThread(FALSE);
            HRESULT dlgEventsResult = CDialogEventHandler_CreateInstance(initialFolderItem, IID_PPV_ARGS(&pfde));
            if (SUCCEEDED(dlgEventsResult) && pfde) {
                pfd->Advise(pfde, &dlgEventsCookie);
            }
            dlgEventsResult = pfd->AddPlace(initialFolderItem, FDAP_TOP);
            pfd->SetFolder(initialFolderItem);
        }

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
				LPWSTR pszDisplayName = NULL;
				psiResult->GetDisplayName(SIGDN_FILESYSPATH, &pszDisplayName);

				data = env->NewStringUTF(ToMbStr(std::wstring(pszDisplayName)).c_str());

				CoTaskMemFree(pszDisplayName);
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

        if(dlgEventsCookie)
            pfd->Unadvise(dlgEventsCookie);
        if(pfde)
            pfde->Release();

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
		free((void*)customFilter.pszName);
	if (customFilter.pszSpec)
		free((void*)customFilter.pszSpec);
	delete[] filters;
	CoUninitialize();
}
