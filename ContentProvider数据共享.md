对于同一应用内的contentProvider不需要任何权限就能访问数据。
那如何才能让其他应用也可以访问此应用中的数据呢，一种方法是向此应用设置一个android:sharedUserId,然后需要访问次数据的应用也设置同一个
sharedUserId,具有相同sharedUserId的应用间可以共享数据。
但是此种方法不够安全，也无法做到对不同数据进行不同读写权限的管理，下面我们就来详细介绍下ContentProvider中的数据共享规则。
首先我们先介绍下，共享数据所涉及到的几个重要标签：
* android:exported 设置此provider是否可以被其他应用使用
* android:readPermission 该Provider的读权限的标识。
* android:writerPermission 该Provider的写权限标识。
* android:permission provider读写权限标识
* android:grantUriPermissions 临时权限标识，true时，意味着该provider下的所有数据均可被临时使用。false时，反之，但可以通过设置<grant-uri-permission>标签来制定哪些路径可以被临时使用。
下面举个例子：
```xml
<permission android:name="me.pengtao.READ" android:protectionLevel="normal"/>
```
然后改变provider标签为
```xml
<provider>
    android:authorities="me.pengtao.contentprovidertest"
    android:name=".provider.TestProvider"
    android:readPermission="me.pengtao.READ"
    android:exported="true"
</provider>
```
则在其他应用中可以使用一下权限来对TestProvider进行访问
```xml
<uses-permission android:name="me.pengtao.READ"/>
```
有人可能又想问，如果我的provider里面包含了不同的数据库表，我希望对不同的数据库表有不同的权限操作，要如何做呢？Android为这种场景提供了provider的子标签
<path-permission>，path-permission包含了一下几个标签
```xml
<path-permission android:path="string"
                 android:pathPrefix="string"
                 android:pathPattern="string"
                 android:permission="string"
                 android:readPermission="string"
                 android:writePermission="string" />
```