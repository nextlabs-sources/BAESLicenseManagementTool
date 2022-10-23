;--------------------------------
;Include Modern UI

	!include "MUI2.nsh"
	!include nsDialogs.nsh
	!include LogicLib.nsh
	!include "FileFunc.nsh"
	!include "TextFunc.nsh"
	

;--------------------------------
;General

  ;Name and file
  Name "License Management Tool-${VERSION}"
  BrandingText "License Management Tool-${VERSION} (NSIS 3.03)"
  OutFile "${DIRECTORY}\..\LicenseManagementTool-${VERSION}.exe"

  ;Default installation folder
  InstallDir "C:\License Management Tool"
  
  ;Get installation folder from registry if available
  InstallDirRegKey HKLM "SOFTWARE\Nextlabs\License Management Tool" ""

  ;Request application privileges
  RequestExecutionLevel admin

;--------------------------------
;Interface Settings
	!define MUI_HEADERIMAGE
	!define MUI_HEADERIMAGE_BITMAP "${DIRECTORY}\..\nextlabs.bmp"
	!define MUI_HEADERIMAGE_RIGHT
	!define MUI_ABORTWARNING
	!define MUI_ICON "${DIRECTORY}\..\nxce.ico"
        !define MUI_UNICON "${DIRECTORY}\..\nxce.ico"
  
;--------------------------------
;Variables

Var /global CheckInstall
Var CheckUpdate
Var CheckUninstall
Var /global CheckUpdateState
Var /global CheckUninstallState
Var /global InstalledDir

Var InstallText
Var InstallSubText
Var /global InstallDirectory

Var BrowseCert
Var SSLCert
Var CertPass
Var SSLPort
Var NonSSLPort
Var /global NonSSLPortText
Var /global SSLPortText
Var /global SSLCertText
Var /global CertPassText
Var NonSSLCheck
Var /global NonSSLCheckState
Var SelfSSLCheck
Var /global SelfSSLCheckState
Var CertSSLCheck
Var /global CertSSLCheckState
Var /global JavaPathText
Var /global SSLOldSetting

Var DBServer
Var DBPort
Var SIDCheck
Var DBDatabaseSID
Var DBUser
Var DBPassword
Var ServiceNameCheck
Var DBDatabaseService

Var /global DBServerText
Var /global DBPortText
Var /global DBDatabaseSIDText
Var /global DBUserText
Var /global DBPasswordText
Var /global DBConnectionString
Var /global DBOK
Var /global DBDatabaseServiceNameText
Var /global SIDCheckState
Var /global ServiceNameCheckState

Var LdapServer
Var LdapPort
Var LdapDomain
Var GroupDomain
Var LdapUser
Var LdapPassword

Var /global LdapServerText
Var /global LdapPortText
Var /global LdapDomainText
Var /global GroupDomainText
Var /global LdapUserText
Var /global LdapPasswordText
Var /global ADOK

Var /global PROPSOK



;--------------------------------
;Pages
	XPStyle on
	Page custom nsWelcome onWelcomeNext
	!insertmacro MUI_PAGE_LICENSE "License.txt"
	!define MUI_PAGE_CUSTOMFUNCTION_PRE dirPre
	!insertmacro MUI_PAGE_DIRECTORY
	!define MUI_INSTFILESPAGE_FINISHHEADER_TEXT "$InstallText"
	!define	MUI_INSTFILESPAGE_FINISHHEADER_SUBTEXT "$InstallSubText"
	!insertmacro MUI_PAGE_INSTFILES	
	Page custom nsCertificate OnCertNext
	Page custom nsDatabase OnDatabaseNext
	Page custom nsLdap onLdapNext
	!insertmacro MUI_PAGE_FINISH
	!insertmacro MUI_UNPAGE_CONFIRM
	!insertmacro MUI_UNPAGE_INSTFILES
  
;--------------------------------
;Languages
  !insertmacro MUI_LANGUAGE "English"
  LangString WelcomePageTitle ${LANG_ENGLISH} "Welcome to License Management Tool Installation."
  LangString WelcomePageSubtitle ${LANG_ENGLISH} "This installation will install License Management Tool into your computer."
  
;--------------------------------
;Custom Pages

Function nsWelcome
	!insertmacro MUI_HEADER_TEXT $(WelcomePageTitle) $(WelcomePageSubtitle)

	nsDialogs::Create 1018
	Pop $0
	
	ReadRegStr $0 HKLM "SOFTWARE\Nextlabs\License Management Tool" ""
	${If} $0 != ""
		
		
		${NSD_CreateLabel} 0 0 100% 39u "The installer detects an existing License Management Tool installation. Please choose your option to proceed."
		${NSD_CreateRadioButton} 0 40 100% 28u "Update the application. This action will make necessary changes in application components, application properties or database"
		Pop $CheckUpdate
		GetFunctionAddress $0 CheckUpdate
		nsDialogs::OnClick $CheckUpdate $0

	
		${NSD_CreateRadioButton} 0 90 100% 28u "Uninstall the application. This will launch License Management Tool Uninstaller."
		Pop $CheckUninstall
		GetFunctionAddress $0 CheckUninstall
		nsDialogs::OnClick $CheckUninstall $0
		
		${NSD_SetState} $CheckUpdate 1
		
		StrCpy $CheckInstall "false"
	${Else} 
		${NSD_CreateLabel} 0 0 100% 12u "This installation will perform the following steps. Please click Next to continue."
		${NSD_CreateLabel} 20 30 100% 12u "1) License Agreement"
		${NSD_CreateLabel} 20 50 100% 12u "2) Directory Selection and File Extraction"
		${NSD_CreateLabel} 20 70 100% 12u "3) Application Server Configuration"
		${NSD_CreateLabel} 20 90 100% 12u "4) Database Configuration"
		${NSD_CreateLabel} 20 110 100% 12u "5) Active Directory Configuration"
		StrCpy $CheckInstall "true"
		${NSD_SetState} $CheckUpdate 0
		${NSD_SetState} $CheckUninstall 0
	${EndIf}
	
	nsDialogs::Show
FunctionEnd

Function CheckUpdate
	${NSD_GetState} $CheckUpdate $CheckUpdateState
	${NSD_GetState} $CheckUninstall $CheckUninstallState
FunctionEnd

Function CheckUninstall
	${NSD_GetState} $CheckUpdate $CheckUpdateState
	${NSD_GetState} $CheckUninstall $CheckUninstallState
FunctionEnd

Function onWelcomeNext
	${NSD_GetState} $CheckUninstall $CheckUninstallState
	${NSD_GetState} $CheckUpdate $CheckUpdateState
	${If} $CheckInstall == "false"
		${If} $CheckUninstallState == 1
			ReadRegStr $0 HKLM "SOFTWARE\Nextlabs\License Management Tool" ""
			Exec "$0\Uninstall.exe"
			Quit
		${EndIf}
	${EndIf}
FunctionEnd

Function dirPre
	${If} $CheckUpdateState == 1
		StrCpy $INSTDIR "$TEMP\Nextlabs\LicenseManagementTool\"
		Abort
	${Else}
		StrCpy $INSTDIR "C:\Nextlabs\LicenseManagementTool\"
	${EndIf}
FunctionEnd

Function nsCertificate

	${If} $CheckUpdateState == 1
		Abort
	${EndIf}

	nsDialogs::Create 1018
	Pop $0
	nsisXML::create
	nsisXML::load "$INSTDIR\server\apache-tomcat-7.0.57\conf\server.xml"
	nsisXML::select '/Server/Service/Connector[@SSLEnabled="true"]'	
	nsisXML::getAttribute "port"
	StrCpy $SSLPortText $3
	nsisXML::release $0
	StrCpy $NonSSLPortText "8080"
	
	GetFunctionAddress $0 OnCertBack
	nsDialogs::OnBack $0
	
	
	${If} $CertSSLCheckState == 1
		${If} $SSLCertText == ""
		${OrIf} $CertPassText == ""	
		${OrIf} $SSLPortText == ""
			GetDlgItem $0 $HWNDPARENT 1
			EnableWindow $0 0
		${EndIf}
	${EndIf}
	
	${If} $SelfSSLCheckState == 1
		${If} $SSLPortText == ""
			GetDlgItem $0 $HWNDPARENT 1
			EnableWindow $0 0
		${EndIf}
	${EndIf}
	
	${If} $NonSSLCheckState == 1
		${If} $NonSSLPortText == ""
			GetDlgItem $0 $HWNDPARENT 1
			EnableWindow $0 0
		${EndIf}
	${EndIf}
	
	${NSD_CreateLabel} 0 0 100% 20u "Select if you want the application to use non-SSL, SSL with self-signed certificate, or SSL with your own certificate."
	
	${NSD_CreateRadioButton} 0 30 100% 14u "Disable SSL. HTTP protocol will be used."
	Pop $NonSSLCheck
	GetFunctionAddress $0 NonSSLCheck
	nsDialogs::OnClick $NonSSLCheck $0

	
	${NSD_CreateRadioButton} 0 60 100% 14u "Enable SSL using a self-signed certificate. HTTPS protocol will be used."
	Pop $SelfSSLCheck
	GetFunctionAddress $0 SelfSSLCheck
	nsDialogs::OnClick $SelfSSLCheck $0
	
	${NSD_CreateRadioButton} 0 90 100% 14u "Enable SSL using your own certificate. HTTPS protocol will be used."
	Pop $CertSSLCheck
	GetFunctionAddress $0 CertSSLCheck
	nsDialogs::OnClick $CertSSLCheck $0
	
	
	${If} $SelfSSLCheckState != 1
	${AndIf} $CertSSLCheckState != 1
		${NSD_SetState} $NonSSLCheck 1
	${Else}
		${NSD_SetState} $NonSSLCheck $NonSSLCheckState
		${NSD_SetState} $SelfSSLCheck $SelfSSLCheckState
		${NSD_SetState} $CertSSLCheck $CertSSLCheckState
	${EndIf}

	${NSD_CreateBrowseButton} 5% 117 40% 13u "Select your keystore"
	Pop $BrowseCert
	GetFunctionAddress $0 BrowseCert
	nsDialogs::OnClick $BrowseCert $0
	
	${NSD_CreateLabel} 50% 120 23% 12u "Keystore Password"
	${NSD_CreatePassword}  74% 117 25% 13u "$CertPassText"
	Pop $CertPass
	GetFunctionAddress $0 OnCertPassChange
	nsDialogs::OnChange $CertPass $0
	
	${If} $CertSSLCheckState != 1
		EnableWindow $BrowseCert 0
		EnableWindow $CertPass 0
	${EndIf}
	
	${NSD_CreateLabel} 5% 140 45% 12u "$SSLCertText"
	Pop $SSLCert
	
	${NSD_CreateLabel} 0 190 15% 12u "Non-SSL Port"
	${NSD_CreateText}  17% 187 23% 13u "$NonSSLPortText"
	Pop $NonSSLPort
	GetFunctionAddress $0 OnNonSSLPortChange
	nsDialogs::OnChange $NonSSLPort $0
	
	${NSD_CreateLabel} 50% 190 14% 12u "SSL Port"
	${NSD_CreateText}  67% 187 23% 13u "$SSLPortText"
	Pop $SSLPort
	GetFunctionAddress $0 OnSSLPortChange
	nsDialogs::OnChange $SSLPort $0
	
	${If} $NonSSLCheckState == 0
		EnableWindow $SSLPort 1
		EnableWindow $NonSSLPort 0
	${Else}
		EnableWindow $NonSSLPort 1
		EnableWindow $SSLPort 0
	${EndIf}
	
	;ReadEnvStr $R0 "JAVA_HOME"
	;${NSD_CreateLabel} 0 190 100% 12u "JAVA_HOME: $R0"

	;ReadEnvStr $R0 "JRE_HOME"
	;${NSD_CreateLabel} 0 210 100% 12u "JRE_HOME: $R1"

	nsDialogs::Show

FunctionEnd

Function NonSSLCheck
	EnableWindow $BrowseCert 0
	EnableWindow $CertPass 0
	EnableWindow $SSLPort 0
	EnableWindow $NonSSLPort 1
	${NSD_GetText} $NonSSLPort $2
	${If} $2 == ""
		GetDlgItem $0 $HWNDPARENT 1
		EnableWindow $0 0
	${Else}
		GetDlgItem $0 $HWNDPARENT 1
		EnableWindow $0 1
	${EndIf}
	${NSD_GetState} $NonSSLCheck $NonSSLCheckState
	${NSD_GetState} $SelfSSLCheck $SelfSSLCheckState
	${NSD_GetState} $CertSSLCheck $CertSSLCheckState
FunctionEnd

Function SelfSSLCheck
	EnableWindow $BrowseCert 0
	EnableWindow $CertPass 0
	EnableWindow $SSLPort 1
	EnableWindow $NonSSLPort 0
	${NSD_GetText} $SSLPort $2
	${If} $2 == ""
		GetDlgItem $0 $HWNDPARENT 1
		EnableWindow $0 0
	${Else}
		GetDlgItem $0 $HWNDPARENT 1
		EnableWindow $0 1
	${EndIf}
	${NSD_GetState} $NonSSLCheck $NonSSLCheckState
	${NSD_GetState} $SelfSSLCheck $SelfSSLCheckState
	${NSD_GetState} $CertSSLCheck $CertSSLCheckState

FunctionEnd

Function CertSSLCheck
	EnableWindow $BrowseCert 1
	EnableWindow $CertPass 1
	EnableWindow $SSLPort 1
	EnableWindow $NonSSLPort 0
	${NSD_GetState} $NonSSLCheck $NonSSLCheckState
	${NSD_GetState} $SelfSSLCheck $SelfSSLCheckState
	${NSD_GetState} $CertSSLCheck $CertSSLCheckState
	${NSD_GetText} $SSLCert $0
	${NSD_GetText} $CertPass $1
	${NSD_GetText} $SSLPort $2
	${If} $0 == ""
	${OrIf} $1 == ""
	${OrIf} $2 == ""
		GetDlgItem $0 $HWNDPARENT 1
		EnableWindow $0 0
	${EndIf}
FunctionEnd

Function BrowseCert

	nsDialogs::SelectFileDialog open "C:\.keystore" "Keystore file (*.keystore)|*.keystore"
	Pop $0
	${NSD_SetText} $SSLCert $0
	${NSD_GetText} $CertPass $1
	${NSD_GetText} $SSLPort $2
	
	${If} $0 != ""
	${AndIf} $1 != ""
	${AndIf} $2 != ""
		GetDlgItem $0 $HWNDPARENT 1
		EnableWindow $0 1
	${Else}
		GetDlgItem $0 $HWNDPARENT 1
		EnableWindow $0 0
	${EndIf}

FunctionEnd

Function OnCertPassChange

	${NSD_GetText} $SSLCert $0
	${NSD_GetText} $CertPass $1
	${NSD_GetText} $SSLPort $2
		
	${If} $0 == ""
	${OrIf} $1 == ""
	${OrIf} $2 == ""
		GetDlgItem $0 $HWNDPARENT 1
		EnableWindow $0 0
	${Else}
		GetDlgItem $0 $HWNDPARENT 1
		EnableWindow $0 1
	${EndIf}

FunctionEnd

Function OnNonSSLPortChange

	${NSD_GetText} $NonSSLPort $2
		
	${If} $2 == ""
		GetDlgItem $0 $HWNDPARENT 1
		EnableWindow $0 0
	${Else}
		GetDlgItem $0 $HWNDPARENT 1
		EnableWindow $0 1
	${EndIf}

FunctionEnd

Function OnSSLPortChange

	${NSD_GetText} $SSLCert $0
	${NSD_GetText} $CertPass $1
	${NSD_GetText} $SSLPort $2
	${NSD_GetState} $CertSSLCheck $3
	
	${If} $3 == 1
		${If} $0 == ""
		${OrIf} $1 == ""
		${OrIf} $2 == ""
			GetDlgItem $0 $HWNDPARENT 1
			EnableWindow $0 0
		${Else}
			GetDlgItem $0 $HWNDPARENT 1
			EnableWindow $0 1
		${EndIf}
	${Else}
		${If} $2 == ""
			GetDlgItem $0 $HWNDPARENT 1
			EnableWindow $0 0
		${Else}
			GetDlgItem $0 $HWNDPARENT 1
			EnableWindow $0 1
		${EndIf}
	${EndIf}

FunctionEnd

Function OnCertBack

FunctionEnd

;On confirming certificate, configure Tomcat service
Function OnCertNext
	ClearErrors
	ReadEnvStr $JavaPathText "JAVA_HOME"
	IfErrors 0 JreFound
	
	ClearErrors
	ReadEnvStr $JavaPathText "JRE_HOME"
	IfErrors 0 JreFound
	MessageBox MB_ICONEXCLAMATION  "Cannot find JAVA_HOME or JRE_HOME. Please install Java before continue."	
	Abort
	JreFound:
		
		${NSD_GetState} $NonSSLCheck $NonSSLCheckState
		${NSD_GetState} $SelfSSLCheck $SelfSSLCheckState
		${NSD_GetState} $CertSSLCheck $CertSSLCheckState
		${NSD_GetText} $NonSSLPort $NonSSLPortText
		${NSD_GetText} $SSLPort $SSLPortText
		
		${If} $NonSSLCheckState == 1
			nsisXML::create
			nsisXML::load "$INSTDIR\server\apache-tomcat-7.0.57\conf\server.xml"
			nsisXML::select '/Server/Service/Connector[@SSLEnabled="true"]'	
			nsisXML::setAttribute "keystoreFile" "$INSTDIR\keystore\.keystore"
			nsisXML::select '/Server/Service/Connector[@port="8080"]'
			nsisXML::setAttribute "port" "$NonSSLPortText"
			nsisXML::save "$INSTDIR\server\apache-tomcat-7.0.57\conf\server.xml"			
			nsisXML::release $0
			
			ExecWait "$INSTDIR\server\apache-tomcat-7.0.57\bin\service_install.bat"
			SimpleSC::StartService "Tomcat7" "" 60
			Pop $0
			
			Sleep 5000	
			
			SimpleSC::StopService "Tomcat7" 1 60
			Pop $0
			
			Sleep 3000
			
			;Delete "$INSTDIR\server\apache-tomcat-7.0.57\webapps\LicenseManagementTool.war"
			;CopyFiles "$INSTDIR\helper\web.xml" "$INSTDIR\server\apache-tomcat-7.0.57\webapps\LicenseManagementTool\WEB-INF\web.xml"	

			nsisXML::create
			nsisXML::load "$INSTDIR\server\apache-tomcat-7.0.57\webapps\LicenseManagementTool\WEB-INF\web.xml"
			nsisXML::select '//*[local-name()="web-app"]//*[local-name()="security-constraint"]//*[local-name()="user-data-constraint"]//*[local-name()="transport-guarantee"]'
			nsisXML::setText "NONE"
			nsisXML::save "$INSTDIR\server\apache-tomcat-7.0.57\webapps\LicenseManagementTool\WEB-INF\web.xml"
			nsisXML::release $0			
			
		${ElseIf} $SelfSSLCheckState == 1
			nsisXML::create
			nsisXML::load "$INSTDIR\server\apache-tomcat-7.0.57\conf\server.xml"
			nsisXML::select '/Server/Service/Connector[@SSLEnabled="true"]'		
			nsisXML::setAttribute "keystoreFile" "$INSTDIR\keystore\.keystore"
			nsisXML::setAttribute "port" "$SSLPortText"
			nsisXML::save "$INSTDIR\server\apache-tomcat-7.0.57\conf\server.xml"
			nsisXML::release $0
			
			ExecWait "$INSTDIR\server\apache-tomcat-7.0.57\bin\service_install.bat"
				
		${Else}
			${NSD_GetText} $SSLCert $SSLCertText
			${NSD_GetText} $CertPass $CertPassText
			
			CopyFiles `$SSLCertText` `$INSTDIR\keystore\cert.keystore`
		
			nsisXML::create
			nsisXML::load "$INSTDIR\server\apache-tomcat-7.0.57\conf\server.xml"
			nsisXML::select '/Server/Service/Connector[@SSLEnabled="true"]'	
			nsisXML::setAttribute "keystoreFile" "$INSTDIR\keystore\cert.keystore"
			nsisXML::setAttribute "keystorePass" "$CertPassText"
			nsisXML::setAttribute "port" "$SSLPortText"
			nsisXML::save "$INSTDIR\server\apache-tomcat-7.0.57\conf\server.xml"
			nsisXML::release $0
			
			ExecWait "$INSTDIR\server\apache-tomcat-7.0.57\bin\service_install.bat"
			
		${EndIf}

		SimpleSC::SetServiceStartType "Tomcat7" "2"
		Pop $0
		
FunctionEnd

Function nsDatabase

	${If} $CheckUpdateState == 1
		Abort
	${EndIf}

	nsDialogs::Create 1018
	Pop $0
	
	${ConfigRead} "$InstallDirectory\properties\LicenseManagementTool.properties" "db-server = " $DBServerText
	${ConfigRead} "$InstallDirectory\properties\LicenseManagementTool.properties" "db-port = " $DBPortText
	${ConfigRead} "$InstallDirectory\properties\LicenseManagementTool.properties" "sql-user = " $DBUserText
	${ConfigRead} "$InstallDirectory\properties\LicenseManagementTool.properties" "sid = " $DBDatabaseSIDText
	${ConfigRead} "$InstallDirectory\properties\LicenseManagementTool.properties" "service-name = " $DBDatabaseServiceNameText
	
	GetFunctionAddress $0 OnDatabaseBack
	nsDialogs::OnBack $0
	
	${If} $SIDCheckState == 1
	      ${If} $DBDatabaseSIDNameText == ""
			GetDlgItem $0 $HWNDPARENT 1
			EnableWindow $0 0
		${EndIf}
	${EndIf}


	${If} $ServiceNameCheckState == 1
		${If} $DBDatabaseServiceNameText == ""
			GetDlgItem $0 $HWNDPARENT 1
			EnableWindow $0 0
		${EndIf}
	${EndIf}

	${NSD_CreateLabel} 0 0 100% 12u "Enter database details."
	
	${NSD_CreateLabel} 10 30 20% 12u "Server"
	${NSD_CreateText}  23% 30 50% 12u "$DBServerText"
	Pop $DBServer
	
	${NSD_CreateLabel} 10 60 20% 12u "Port"
	${NSD_CreateText}  23% 60 50% 12u "$DBPortText"
	Pop $DBPort
	
	${NSD_CreateRadioButton} 10 90 20% 12u "SID"
	Pop $SIDCheck
	GetFunctionAddress $0 SIDCheck
	nsDialogs::OnClick $SIDCheck $0
	
	${NSD_CreateText}  23% 90 50% 12u "$DBDatabaseSIDText"
	Pop $DBDatabaseSID
	
	${NSD_CreateRadioButton} 10 120 20% 12u "Service Name"
	Pop $ServiceNameCheck
	GetFunctionAddress $0 ServiceNameCheck
	nsDialogs::OnClick $ServiceNameCheck $0
	
	${NSD_CreateText}  23% 120 50% 12u "$DBDatabaseServiceNameText"
	Pop $DBDatabaseService
	
	${If} $ServiceNameCheckState != 1
		${NSD_SetState} $SIDCheckState 1
		${NSD_Check} $SIDCheck
	${Else}
		${NSD_SetState} $ServiceNameCheck $ServiceNameCheckState
		${NSD_SetState} $SIDCheck $SIDCheckState
	${EndIf}
	
	${NSD_CreateLabel} 10 150 20% 12u "User"
	${NSD_CreateText}  23% 150 50% 12u "$DBUserText"
	Pop $DBUser
	
	${NSD_CreateLabel} 10 180 20% 12u "Password"
	${NSD_CreatePassword}  23% 180 50% 12u "$DBPasswordText"
	Pop $DBPassword
	
	${If} $SIDCheckState == 0
		EnableWindow $DBDatabaseService 1
		EnableWindow $DBDatabaseSID 0
	${Else}
		EnableWindow $DBDatabaseSID 1
		EnableWindow $DBDatabaseService 0
	${EndIf}

	nsDialogs::Show

FunctionEnd

Function OnDatabaseBack
	${NSD_GetText} $DBServer $DBServerText
	${NSD_GetText} $DBPort $DBPortText
	${NSD_GetState} $SIDCheck $SIDCheckState
	${NSD_GetText} $DBDatabaseSID $DBDatabaseSIDText
	${NSD_GetState} $ServiceNameCheck $ServiceNameCheckState
	${NSD_GetText} $DBDatabaseService $DBDatabaseServiceNameText
	${NSD_GetText} $DBUser $DBUserText
	${NSD_GetText} $DBPassword $DBPasswordText
	SimpleSC::StopService "Tomcat7" 1 60
	Pop $0
	
	Sleep 3000
FunctionEnd

Function SIDCheck
	EnableWindow $DBDatabaseService 0
	EnableWindow $DBDatabaseSID 1
	${NSD_GetText} $DBDatabaseSID $2
	${If} $2 == ""
		GetDlgItem $0 $HWNDPARENT 1
		EnableWindow $0 0
	${Else}
		GetDlgItem $0 $HWNDPARENT 1
		EnableWindow $0 1
	${EndIf}
	${NSD_GetState} $SIDCheck $SIDCheckState
	${NSD_GetState} $ServiceNameCheck $ServiceNameCheckState
FunctionEnd

Function ServiceNameCheck
	EnableWindow $DBDatabaseSID 0
	EnableWindow $DBDatabaseService 1
	${NSD_GetText} $DBDatabaseService $2
	${If} $2 == ""
		GetDlgItem $0 $HWNDPARENT 1
		EnableWindow $0 0
	${Else}
		GetDlgItem $0 $HWNDPARENT 1
		EnableWindow $0 1
	${EndIf}
	${NSD_GetState} $SIDCheck $SIDCheckState
	${NSD_GetState} $ServiceNameCheck $ServiceNameCheckState
FunctionEnd

Function OnDatabaseNext

	${NSD_GetText} $DBServer $DBServerText
	${NSD_GetText} $DBPort $DBPortText
	${NSD_GetText} $DBDatabaseSID $DBDatabaseSIDText
	${NSD_GetText} $DBDatabaseService $DBDatabaseServiceNameText
	${NSD_GetText} $DBUser $DBUserText
	${NSD_GetText} $DBPassword $DBPasswordText
	${NSD_GetState} $SIDCheck $SIDCheckState
	${NSD_GetState} $ServiceNameCheck $ServiceNameCheckState
	
	${If} $ServiceNameCheckState != 1
		StrCpy "$DBConnectionString" ""
		StrCpy "$DBConnectionString" "$DBConnectionStringjdbc:oracle:thin:@" 
		StrCpy "$DBConnectionString" "$DBConnectionString$DBServerText"
		StrCpy "$DBConnectionString" "$DBConnectionString:"
		StrCpy "$DBConnectionString" "$DBConnectionString$DBPortText" 
		StrCpy "$DBConnectionString" "$DBConnectionString:"
		StrCpy "$DBConnectionString" "$DBConnectionString$DBDatabaseSIDText"
	${Else}
		StrCpy "$DBConnectionString" ""
		StrCpy "$DBConnectionString" "$DBConnectionStringjdbc:oracle:thin:@//" 
		StrCpy "$DBConnectionString" "$DBConnectionString$DBServerText"
		StrCpy "$DBConnectionString" "$DBConnectionString:"
		StrCpy "$DBConnectionString" "$DBConnectionString$DBPortText" 
		StrCpy "$DBConnectionString" "$DBConnectionString/"
		StrCpy "$DBConnectionString" "$DBConnectionString$DBDatabaseServiceNameText"
	${EndIf}
		
	StrCpy $0 "testdb" 
	StrCpy $1 "oracle.jdbc.driver.OracleDriver"	
	
	ExecWait '$JavaPathText\bin\javaw.exe -jar "$InstallDirectory\helper\lmt-connection.jar" "$0" "$1" "$DBConnectionString" "$DBUserText" "$DBPasswordText"' $DBOK
	
	${If} $DBOK != 0
		MessageBox MB_ICONEXCLAMATION  "Connection Failed! Please check your inputs."
		Abort
	${Else}
		${ConfigWrite} "$InstallDirectory\properties\LicenseManagementTool.properties" "db-server = " $DBServerText $0
		${ConfigWrite} "$InstallDirectory\properties\LicenseManagementTool.properties" "db-port = " $DBPortText $0
		${ConfigWrite} "$InstallDirectory\properties\LicenseManagementTool.properties" "sql-user = " $DBUserText $0
		${ConfigWrite} "$InstallDirectory\properties\LicenseManagementTool.properties" "sql-password = " $DBPasswordText $0
		${ConfigWrite} "$InstallDirectory\properties\LicenseManagementTool.properties" "sid = " $DBDatabaseSIDText $0
		${ConfigWrite} "$InstallDirectory\properties\LicenseManagementTool.properties" "service-name = " $DBDatabaseServiceNameText $0
		${ConfigWrite} "$InstallDirectory\properties\LicenseManagementTool.properties" "connection-string = " $DBConnectionString $0
	${EndIf}
	
	StrCpy $0 "checktablesexist" 
	StrCpy $1 "oracle.jdbc.driver.OracleDriver"		
	ExecWait '$JavaPathText\bin\javaw.exe -jar "$InstallDirectory\helper\lmt-connection.jar" "$0" "$1" "$DBConnectionString" "$DBUserText" "$DBPasswordText"' $DBOK
	
	${If} $DBOK == 1
		MessageBox MB_YESNO "Tables exist in the specified database. Do you want to drop the current tables and create new ones? By clicking No, old tables will be used." IDYES dropAndCreate IDNO keep
		dropAndCreate:
			StrCpy $0 "droptables"
			StrCpy $1 "oracle.jdbc.driver.OracleDriver"
			StrCpy $2 "$InstallDirectory\helper\drop.sql"	
			ExecWait '$JavaPathText\bin\javaw.exe -jar "$InstallDirectory\helper\lmt-connection.jar" "$0" "$1" "$DBConnectionString" "$DBUserText" "$DBPasswordText" "$2"' $DBOK
			${If} $DBOK != 0
				MessageBox MB_ICONEXCLAMATION  "Cannot drop tables. Please check installation log."
				Abort
			${EndIf}
			
			Goto createTable
		keep:		
			Goto end
	${Else}
		Goto createTable
	${EndIf}
	createTable:
		StrCpy $0 "createtables"
		StrCpy $1 "oracle.jdbc.driver.OracleDriver"
		StrCpy $2 "$InstallDirectory\helper\tables.sql"
		ExecWait '$JavaPathText\bin\javaw.exe -jar "$InstallDirectory\helper\lmt-connection.jar" "$0" "$1" "$DBConnectionString" "$DBUserText" "$DBPasswordText" "$2"' $DBOK
		${If} $DBOK != 0
			MessageBox MB_ICONEXCLAMATION  "Cannot create tables. Please check installation log."
			Abort
		${EndIf}
		Goto end
	end:
FunctionEnd

Function nsLdap
	
	${If} $CheckUpdateState == 1
		Abort
	${EndIf}

	nsDialogs::Create 1018
	Pop $0
	
	GetFunctionAddress $0 OnLdapBack
	nsDialogs::OnBack $0
	
	${ConfigRead} "$InstallDirectory\properties\LicenseManagementTool.properties" "ad-server = " $LdapServerText
	${ConfigRead} "$InstallDirectory\properties\LicenseManagementTool.properties" "ad-port = " $LdapPortText
	${ConfigRead} "$InstallDirectory\properties\LicenseManagementTool.properties" "ldap-domain-name = " $LdapDomainText
	${ConfigRead} "$InstallDirectory\properties\LicenseManagementTool.properties" "group-domain-name = " $GroupDomainText
	${ConfigRead} "$InstallDirectory\properties\LicenseManagementTool.properties" "edit-account-name = " $LdapUserText

	${NSD_CreateLabel} 0 0 100% 12u "Enter LDAP details."
	
	${NSD_CreateLabel} 10 30 28% 12u "Server"
	${NSD_CreateText}  30% 30 50% 12u "$LdapServerText"
	Pop $LdapServer
	
	${NSD_CreateLabel} 10 60 28% 12u "Port"
	${NSD_CreateText}  30% 60 50% 12u "$LdapPortText"
	Pop $LdapPort
	
	${NSD_CreateLabel} 10 90 28% 12u "User Domain"
	${NSD_CreateText}  30% 90 50% 12u "$LdapDomainText"
	Pop $LdapDomain
	
	${NSD_CreateLabel} 10 120 28% 12u "License Group Domain"
	${NSD_CreateText}  30% 120 50% 12u "$GroupDomainText"
	Pop $GroupDomain
	
	${NSD_CreateLabel} 10 150 28% 12u "Master Account"
	${NSD_CreateText}  30% 150 50% 12u "$LdapUserText"
	Pop $LdapUser
	
	${NSD_CreateLabel} 10 180 28% 12u "Password"
	${NSD_CreatePassword}  30% 180 50% 12u "$LdapPasswordText"
	Pop $LdapPassword

	nsDialogs::Show
FunctionEnd

Function onLdapBack
	${NSD_GetText} $LdapServer $LdapServerText
	${NSD_GetText} $LdapPort $LdapPortText
	${NSD_GetText} $LdapDomain $LdapDomainText
	${NSD_GetText} $GroupDomain $GroupDomainText
	${NSD_GetText} $LdapUser $LdapUserText
	${NSD_GetText} $LdapPassword $LdapPasswordText
FunctionEnd

Function onLdapNext

	${NSD_GetText} $LdapServer $LdapServerText
	${NSD_GetText} $LdapPort $LdapPortText
	${NSD_GetText} $LdapDomain $LdapDomainText
	${NSD_GetText} $GroupDomain $GroupDomainText
	${NSD_GetText} $LdapUser $LdapUserText
	${NSD_GetText} $LdapPassword $LdapPasswordText
	
	StrCpy $0 "testad"
	
	ExecWait '$JavaPathText\bin\javaw.exe -jar "$InstallDirectory\helper\lmt-connection.jar" "$0" "$LdapDomainText" "$LdapServerText" $LdapPortText "$LdapUserText" "$LdapPasswordText"' $ADOK
	
	${If} $ADOK != 0
		MessageBox MB_ICONEXCLAMATION  "Connection Failed! Please check your inputs."
		Abort
	${EndIf}
	
	${ConfigWrite} "$InstallDirectory\properties\LicenseManagementTool.properties" "ad-server = " $LdapServerText $0
	${ConfigWrite} "$InstallDirectory\properties\LicenseManagementTool.properties" "ad-port = " $LdapPortText $0
	${ConfigWrite} "$InstallDirectory\properties\LicenseManagementTool.properties" "ldap-domain-name = " $LdapDomainText $0
	${ConfigWrite} "$InstallDirectory\properties\LicenseManagementTool.properties" "group-domain-name = " $GroupDomainText $0
	${ConfigWrite} "$InstallDirectory\properties\LicenseManagementTool.properties" "edit-account-name = " $LdapUserText $0
	${ConfigWrite} "$InstallDirectory\properties\LicenseManagementTool.properties" "edit-account-password = " $LdapPasswordText $0
	
	SimpleSC::StartService "Tomcat7" "" 60
	Pop $0
	
	Sleep 3000
FunctionEnd

;--------------------------------
;Installer Sections

Section "Install File"
	StrCpy "$InstallDirectory" $INSTDIR
	
	SetOutPath "$INSTDIR"
  
	File /r "${DIRECTORY}\*"
	
	${If} $CheckUpdateState == 0
		StrCpy "$InstallText" "Extraction Completed"
		StrCpy "$InstallSubText" "Files have been extracted to destination directory. Please continue configuring the application."
		
		;Store installation folder
		WriteRegStr HKLM "SOFTWARE\Nextlabs\License Management Tool" "" $INSTDIR
	
		;Create uninstaller
		WriteUninstaller "$INSTDIR\Uninstall.exe"
        DetailPrint "Start editing LicenseManagementTool.xml"
		nsisXML::create
		nsisXML::load "$INSTDIR\server\apache-tomcat-7.0.57\conf\Catalina\localhost\LicenseManagementTool.xml"
		nsisXML::select '/Context/Parameter[@name="BAESToolProperties"]'
		nsisXML::setAttribute "value" "$INSTDIR\properties\LicenseManagementTool.properties"
		nsisXML::select '/Context/Parameter[@name="BAESToolConstant"]'
		nsisXML::setAttribute "value" "$INSTDIR\properties\LicenseManagementToolConstant.properties"	
		nsisXML::save "$INSTDIR\server\apache-tomcat-7.0.57\conf\Catalina\localhost\LicenseManagementTool.xml"
		DetailPrint "Save editing LicenseManagementTool.xml"
		nsisXML::release $0
		DetailPrint "Finish editing LicenseManagementTool.xml"
	${Else}
		SimpleSC::StopService "Tomcat7" 1 60
		Pop $0
		
		Sleep 3000
		
		ReadRegStr $InstalledDir HKLM "SOFTWARE\Nextlabs\License Management Tool" ""
		
		ReadEnvStr $JavaPathText "JAVA_HOME"
		${If} $JavaPathText == ""
			ReadEnvStr $JavaPathText "JRE_HOME"
		${EndIf}
		
		${ConfigRead} "$InstalledDir\properties\LicenseManagementTool.properties" "sql-user = " $DBUserText
		${ConfigRead} "$InstalledDir\properties\LicenseManagementTool.properties" "sql-password = " $DBPasswordText
		${ConfigRead} "$InstalledDir\properties\LicenseManagementTool.properties" "connection-string = " $DBConnectionString
		
		nsisXML::create
		nsisXML::load "$InstalledDir\server\apache-tomcat-7.0.57\webapps\LicenseManagementTool\WEB-INF\web.xml"
		nsisXML::select '//*[local-name()="web-app"]//*[local-name()="security-constraint"]//*[local-name()="user-data-constraint"]//*[local-name()="transport-guarantee"]'
		nsisXML::getText
		StrCpy $SSLOldSetting "$3"
		nsisXML::save "$InstalledDir\server\apache-tomcat-7.0.57\webapps\LicenseManagementTool\WEB-INF\web.xml"
		nsisXML::release $0
		
		CopyFiles "$INSTDIR\server\apache-tomcat-7.0.57\webapps\LicenseManagementTool.war" "$InstalledDir\server\apache-tomcat-7.0.57\webapps\LicenseManagementTool.war"
		CopyFiles "$INSTDIR\helper\lmt-connection.jar" "$InstalledDir\helper\lmt-connection.jar"
		CopyFiles "$INSTDIR\properties\LicenseManagementToolConstant.properties" "$InstalledDir\properties\LicenseManagementToolConstant.properties"
		RMDir /r "$InstalledDir\server\apache-tomcat-7.0.57\webapps\LicenseManagementTool"
		
		StrCpy $0 "updateproperties"
	
		ExecWait '$JavaPathText\bin\javaw.exe -jar "$InstallDirectory\helper\lmt-connection.jar" "$0" "$InstalledDir\properties\LicenseManagementTool.properties" "$InstallDirectory\properties\LicenseManagementTool.properties"' $PROPSOK
		${If} $PROPSOK != 0
			CopyFiles "$INSTDIR\log\*.*" "$InstalledDir\log"
			MessageBox MB_ICONEXCLAMATION  "Cannot update properties file. Please check installation log."	
			Abort
		${EndIf}
		
		StrCpy $0 "altertables"
		StrCpy $1 "oracle.jdbc.driver.OracleDriver"
		StrCpy $2 "$InstallDirectory\helper\alter.sql"	
		ExecWait '$JavaPathText\bin\javaw.exe -jar "$InstallDirectory\helper\lmt-connection.jar" "$0" "$1" "$DBConnectionString" "$DBUserText" "$DBPasswordText" "$2"' $DBOK
		${If} $DBOK != 0
			CopyFiles "$INSTDIR\log\*.*" "$InstalledDir\log"
			MessageBox MB_ICONEXCLAMATION  "Cannot alter tables. Please check installation log."
			Abort
		${EndIf}
		
		CopyFiles "$INSTDIR\log\*.*" "$InstalledDir\log"
		RMDir /r "$INSTDIR"
		
		SimpleSC::StartService "Tomcat7" "" 60
		Pop $0
		
		Sleep 5000
		
		SimpleSC::StopService "Tomcat7" 1 60
		Pop $0
		
		Sleep 3000
		
		nsisXML::create
		nsisXML::load "$InstalledDir\server\apache-tomcat-7.0.57\webapps\LicenseManagementTool\WEB-INF\web.xml"
		nsisXML::select '//*[local-name()="web-app"]//*[local-name()="security-constraint"]//*[local-name()="user-data-constraint"]//*[local-name()="transport-guarantee"]'
		nsisXML::setText "$SSLOldSetting"
		nsisXML::save "$InstalledDir\server\apache-tomcat-7.0.57\webapps\LicenseManagementTool\WEB-INF\web.xml"
		nsisXML::release $0
		
		SimpleSC::StartService "Tomcat7" "" 60
		
		Pop $0
	${EndIf}

SectionEnd

;--------------------------------
;Uninstaller Section

Section "Uninstall"

	SimpleSC::StopService "Tomcat7" 1 60
	Pop $0
	
	Sleep 3000
  
	ExecWait "$INSTDIR\server\apache-tomcat-7.0.57\bin\service_remove.bat"
  
	${ConfigRead} "$INSTDIR\properties\LicenseManagementTool.properties" "sql-user = " $DBUserText
	${ConfigRead} "$INSTDIR\properties\LicenseManagementTool.properties" "sql-password = " $DBPasswordText
	${ConfigRead} "$INSTDIR\properties\LicenseManagementTool.properties" "connection-string = " $DBConnectionString
	${ConfigRead} "$INSTDIR\properties\LicenseManagementTool.properties" "connection-string = " $DBConnectionString
	
	ReadEnvStr $JavaPathText "JAVA_HOME"
	${If} $JavaPathText == ""
		ReadEnvStr $JavaPathText "JRE_HOME"
	${EndIf}
	
	StrCpy $0 "checktablesexist" 
	StrCpy $1 "oracle.jdbc.driver.OracleDriver"		
	ExecWait '$JavaPathText\bin\javaw.exe -jar "$INSTDIR\helper\lmt-connection.jar" "$0" "$1" "$DBConnectionString" "$DBUserText" "$DBPasswordText"' $DBOK
	
	${If} $DBOK == 1
		MessageBox MB_YESNO "Do u want to drop all tables in database?" IDYES drop IDNO keep
		drop:
			StrCpy $0 "droptables"
			StrCpy $1 "oracle.jdbc.driver.OracleDriver"
			StrCpy $2 "$INSTDIR\helper\drop.sql"	
			ExecWait '$JavaPathText\bin\javaw.exe -jar "$INSTDIR\helper\lmt-connection.jar" "$0" "$1" "$DBConnectionString" "$DBUserText" "$DBPasswordText" "$2"' $DBOK
			${If} $DBOK != 0
				MessageBox MB_ICONEXCLAMATION  "Cannot drop tables. Please check installation log."
				
				Abort
			${EndIf}
			Goto next
		keep:
			Goto next
	${Else}
		Goto next
	${EndIf}	
		
	next:
		Delete "$INSTDIR\Uninstall.exe"
		RMDir /r "$INSTDIR"
		DeleteRegKey /ifempty HKLM "SOFTWARE\Nextlabs\License Management Tool"

SectionEnd