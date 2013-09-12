; gdl-editor-installer.nsi
;
; This script is based on example1.nsi, but it remember the directory, 
; has uninstall support and (optionally) installs start menu shortcuts.
;
; It will install gdl-editor-installer.nsi into a directory that the user selects,

;--------------------------------

; The name of the installer
Name "GDL editor"

; The file to write
OutFile "../../../../dist/gdl-editor/gdl-editor-installer.exe"

; The default installation directory
InstallDir $PROGRAMFILES\GDLEditor

;License
LicenseData "..\..\..\..\LICENSE.txt"

; Registry key to check for directory (so if you install again, it will 
; overwrite the old one automatically)
InstallDirRegKey HKLM "Software\NSIS_GDL_editor" "Install_Dir"

; Request application privileges for Windows Vista
RequestExecutionLevel admin

!macro WriteToFile NewLine File String
  !if `${NewLine}` == true
  Push `${String}$\r$\n`
  !else
  Push `${String}`
  !endif
  Push `${File}`
  Call WriteToFile
!macroend
!define WriteToFile `!insertmacro WriteToFile false`
!define WriteLineToFile `!insertmacro WriteToFile true`

;--------------------------------

; Pages
Page license
Page components
Page directory
Page instfiles

UninstPage uninstConfirm
UninstPage instfiles

;--------------------------------


Var USER_CONFIG_FOLDER
Var CLINICAL_CONTENT_FOLDER
Var MYFOLDER

Function .onInit
	Call GetMyDocs
	StrCpy $MYFOLDER $0
	StrCpy $USER_CONFIG_FOLDER "$PROFILE\.gdleditor"
	StrCpy $CLINICAL_CONTENT_FOLDER "$MYFOLDER\clinical-content"
FunctionEnd


; The stuff to install
Section "GDL editor (required)"
  
  SectionIn RO
  
  ; Set output path to the installation directory.
  SetOutPath $INSTDIR
  
  ; Get compiled files
  File /r "..\..\..\target\gdl-editor\*" ;MAKE SURE you extracted the zip to the 'gdl-editor' folder
  
  
  
  ; Write the installation path into the registry
  WriteRegStr HKLM SOFTWARE\NSIS_GDL_editor "Install_Dir" "$INSTDIR"

  ;Write user config
  SetOutPath $PROFILE\.gdleditor
  Delete "UserConfig.properties"
  ${WriteLineToFile} "$USER_CONFIG_FOLDER\UserConfig.properties" "ArchetypesFolder=$CLINICAL_CONTENT_FOLDER\archetypes"
  ${WriteLineToFile} "$USER_CONFIG_FOLDER\UserConfig.properties" "TemplatesFolder=$CLINICAL_CONTENT_FOLDER\templates"
  ${WriteLineToFile} "$USER_CONFIG_FOLDER\UserConfig.properties" "TerminologiesFolder=$CLINICAL_CONTENT_FOLDER\terminologies"
  ${WriteLineToFile} "$USER_CONFIG_FOLDER\UserConfig.properties" "OntologiesFolder=$CLINICAL_CONTENT_FOLDER\ontologies"
  ${WriteLineToFile} "$USER_CONFIG_FOLDER\UserConfig.properties" "GuidesFolder=$CLINICAL_CONTENT_FOLDER\guidelines"
  ${WriteLineToFile} "$USER_CONFIG_FOLDER\UserConfig.properties" "DocumentsFolder=$INSTDIR\docs"

  
  ; Write the uninstall keys for Windows
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\GDLEditor" "DisplayName" "NSIS GDLEditor"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\GDLEditor" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\GDLEditor" "NoModify" 1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\GDLEditor" "NoRepair" 1
  WriteUninstaller "uninstall.exe"
  
SectionEnd

; Optional section (can be disabled by the user)
Section "Repositories (required)"
  SectionIn RO
  
  SetOutPath $CLINICAL_CONTENT_FOLDER
  File /r "..\..\..\..\..\cm\archetypes"
  File /r "..\..\..\..\..\cm\templates"
  File /r "..\..\..\..\..\cm\terminologies"
  File /r "..\..\..\..\..\cm\ontologies"
  File /r "..\..\..\..\..\cm\guidelines"
SectionEnd


; Optional section (can be disabled by the user)
Section "Start Menu Shortcuts"
  CreateDirectory "$SMPROGRAMS\GDLEditor"
  CreateShortCut "$SMPROGRAMS\GDLEditor\Uninstall.lnk" "$INSTDIR\uninstall.exe" "" "$INSTDIR\uninstall.exe" 0
  CreateShortCut "$SMPROGRAMS\GDLEditor\GDLEditor.lnk" "$INSTDIR\gdl-editor.exe" "" "$INSTDIR\gdl-editor.exe" 0
SectionEnd

; Register file extension GDL
Section "Register GDL file extension"
	WriteRegStr HKCR ".gdl" "" "GDLEditor.File"
	WriteRegStr HKCR "GDLEditor.File" "" "GDLEditor File"
	WriteRegStr HKCR "GDLEditor.File\DefaultIcon" "" "$INSTDIR\gdl.ico"
	WriteRegStr HKCR "GDLEditor.File\shell\open\command" "" '"$INSTDIR\gdl-editor.exe" "%1"'
	WriteRegStr HKCR "GDLEditor.File\shell\print\command" "" '"$INSTDIR\gdl-editor.exe" /p "%1"'
SectionEnd

;--------------------------------

; Uninstaller

Section "Uninstall"
  
  ; Remove registry keys
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\GDLEditor"
  DeleteRegKey HKLM SOFTWARE\NSIS_GDLEditor

  ; Remove files and uninstaller
  RMDir /r $INSTDIR

  ; Remove shortcuts, if any
  Delete "$SMPROGRAMS\GDLEditor\*.*"

  ; Remove directories used
  RMDir "$SMPROGRAMS\GDLEditor"
  RMDir /r "$INSTDIR"

SectionEnd

Function WriteToFile
Exch $0 ;file to write to
Exch
Exch $1 ;text to write
 
  FileOpen $0 $0 a #open file
  FileSeek $0 0 END #go to end
  FileWrite $0 $1 #write to file
  FileClose $0
 
Pop $1
Pop $0
FunctionEnd

; This Function will display "My Documents" Folder path.
Function "GetMyDocs"
  ReadRegStr $0 HKCU \
             "SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\Shell Folders" Personal
FunctionEnd