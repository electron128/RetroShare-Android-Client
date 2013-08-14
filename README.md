# RetroShare Client for Android #

A Client for retroshare-nogui(ssh).

[Download RetroShareAndroidIntegration apk](http://efesto.eigenlab.org/~gioacchino/RetroShareAndroidIntegration/apk/?C=M;O=D)


## How to run from source ##
you need:

	- Android Studio (http://developer.android.com/sdk/installing/studio.html)
	- git (http://git-scm.com/)
	- ant (http://ant.apache.org/)
	- The other tools ( JVM, unzip, wget ) are usually already installed on almost all android developer computers.

Clone RetroShare Android Client source running:

	# git clone https://github.com/G10h4ck/RetroShare-Android-Client.git

Prepare library dependency:

	## If you miss some command install it ;)
	# cd RetroShare-Android-Client/lib
	# wget http://lag.net/jaramiko/download/jaramiko-151.zip
	# unzip jaramiko-151.zip
	# cd jaramiko-151
	# ant jar

Create Android Studio Project:

	From Android Studio main window:
		File -> Import Project -> select RetroShare-Android-Client/RetroShareAndroidIntegration
		File -> Import Module -> select RetroShare-Android-Client/lib/rsctrl/rsctrl
		File -> Project Structure -> Libraries -> + -> Java -> select RetroShare-Android-Client/lib/jaramiko-151/jaramiko.jar ( Doing this you will probably asked what modules of your project depends on that library, select the one created importing RetroShare-Android-Client/RetroShareAndroidIntegration )
		File -> Project Structure -> Libraries -> + -> From Maven... -> put com.google.protobuf in the search box -> press search -> select version 2.4.1 -> set Download to RetroShare-Android-Client/lib -> press OK
		File -> Project Structure -> Libraries -> + -> From Maven... -> put com.google.zxing in the search box -> press search -> select version core:2.0 -> set Download to RetroShare-Android-Client/lib -> press OK
		File -> Project Structure -> Libraries -> + -> From Maven... -> put org.apache.commons.lang in the search box -> press search -> select a version >= 2.6.0 -> set Download to RetroShare-Android-Client/lib -> press OK
Now you should be ready to contribute to the project editing the source and tunning it for testing on your emulator

## What is RetroShare? ##

[RetroShare](http://retroshare.sourceforge.net) is a secure friend-2-friend social network.

