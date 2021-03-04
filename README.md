# DevSec
An android app to monitor system behavior from side-channel information.

## Main files
### MainActivity.java*
This is the entry file of the application.   
The whole logic is :  
1. Ask for 'Usage Stats' permisson, and do items check if applicable.
2. Ask for registration if applicable.
3. If registered but not finish the trial mode, turn to the trial mode if applicable.
4. Once finish the trail mode, application will schedule a foreground service to start scanning.

### PermissionRequire.java
Our app need "Usage Stats\" permisson. This class is to require the permisson.

### ItemsCheck.java
This class will show some clauses to the user.

### Register.java(RSA.java)
This class is for user to register with their email, and then they can upload the database. RSA.java and Base64Utils.java are used to encrypte the user email.

### TimeManager.java(SocketHttpRequester.java Base64Utils.java)
TimeManager.java can schedule a timer to upload the dataset or logs in an interval. SocketHttpRequester.java is the class to send data.  Base64Utils.java is used  to encode data.

### TrialModel.java* and TrialModelStages.java* AfterTrialModel.java
When first entering the app, the app will turn to the Trial mode. The trial mode is for eliminating unavailable functions.  

### SideChannelJob.java*
This is the foreground service class to start scanning and collection.  

### JobInsertRunnable.java (CompilerValue.java,SideChannelValue.java,FrontAppValue.java,GroundTruthValue.java,UserFeedback.java)
Once collect 1020 sets of data(5 types), start a thread to save them into database.  

### CacheScan.java*
This class contains functions:
1. initializing the scanning thread.(private void init(Context mContext))
2. sending notifications.(void Notify(Context mContext))

### NotificationClickReceiver.java
Answer to notification

### LogcatHelper.java CrashHandler.java
This is for log collection.

### Utils.java
This class contains contains some tools for simple processing.

### Classifier.java
This is for online classification, not be used anymore.

-------------------------------------------------------------------------------------------


### native-lib.cpp*
This file contains interface functions with java, such as initializing the setting(Java_com_SMU_DevSec_CacheScan_init), scanning(Java_com_SMU_DevSec_SideChannelJob_sca), trial mode check(Java_com_SMU_DevSec_TrialModelStages_trial1).   

### CheckFlags.cpp
This file contains all tools functions for android side, such as Java_com_SMU_DevSec_CacheScan_GetPattern to get the activated pattern etc.

### libflush*
this is the armageddon lib, which is to get the access time of some functions. If the access time of a function is less than threshold, we think it is activated.

### ReadOffset.cpp, oat-parser lib and dexinfo lib
They are used to parse the oat file and dexfile to get the addresses of functions in the memory.






