1. android.os.Process.killProcess(android.os.Process.myPid());这个只能关闭当前的Activity，也就是对于一个只有单个Activity的应用程序有效，如果对于多个Activity的应用程序他就无能为力了。
System.exit(0);
2. 首先说明该方法运行在Android1.5API Level位3以上才可以，同时需要权限android.permission.RESTRAT_PACKAGES，我们直接结束自己的package即可，直接使用Activitymanager类的restartpackage方法即可
3. ActivityManager.killBackgroundProcesses(String packageName);也可以结束进程,需要权限android.permission.KILL_BACKGROUND_PROCESSES"