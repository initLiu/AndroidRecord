1.Action test
一条<Intent-filter>至少包含一个<action>，否者任何Intent请求都不能和<Intent-filter>匹配。如果Intent请求的Action和<Intent-filter>中某一条<action>匹配，那么该Intent就通过了测试。如果Intent请求中没有设定Action类型，那么只要<Intent-filter>中包含<action>，这个Intent请求就将顺利通过测试。

2.Category test

`

    <intent-filter>
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        ...
    </intent-filter>
`

只有当Intent请求中所有的Category与组件中某一个IntentFilter的<category>完全匹配是，才会让该Intent请求通过测试，IntentFile中多余的<category>声明并不会导致匹配失败。一个没有指定任何category的IntentFilter仅仅只会匹配没有设置category的Intent请求。

3.数据测试

`

    <intent-filter>
        <data android:mimeType="video/mpeg" android:scheme="http" ... />
        <data android:mimeType="audio/mpeg" android:scheme="http" ... />
        ...
    </intent-filter>

`

<data>元素指定了希望接受的Intent请求的数据URI和数据类型，URI被分成三部分来进行匹配：scheme、authority和path。其中，用setData()设定的Inteat请求的URI数据类型和scheme必须与IntentFilter中所指定的一致。若IntentFilter中还指定了authority或path，它们也需要相匹配才会通过测试。