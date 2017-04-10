Fragment生命周期方法包括
```java
onAttach
onCreate
onCreateView
onActivityCreate
onStart
onResume
onPause
onSaveInstanceState
onStop
onDestroyView
onDestroy
onDetach
```
<font color=red>如果把Fragment添加到back stack，按返回键从back stack退出时，fragment并不会被销毁，只是view被destroy。</font>


Fragment中一个方法
```java
Control whether a fragment instance is retained across Activity re-creation (such as from a configuration change). This can only be used with fragments not in the back stack. If set, the fragment lifecycle will be slightly different when an activity is recreated: 
//只有在fragment没有加入到back stack时才有效
public void setRetainInstance (boolean retain)
```
此方法可以改变Fragment的生命周期方法调用，如果retain设置为true，那么在Activity重建(如configuration改变)时，
* Fragment的onDestroy不会被调用，但是onDetach仍然会被调用，因为fragment正在和Activitydetach。
* onCreate不会被调用，因为fragment不会被重新创建
* onAttach和onActivityCreate 仍然会被调用

Fragment也可以通过onSaveInstanceState()方法来保存状态，可以在onCreate(),onCreateView(),onActivityCreate()中恢复状态。

View中所有实现了onSaveInstanceState()方法和onRestoreInstanceState()方法并且设置了id的view都能够自动保存View的状态。