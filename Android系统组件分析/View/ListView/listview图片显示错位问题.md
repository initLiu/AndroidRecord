## Listview图片加载错位问题是由于ListView的缓存机制导致的
## 原因分析：
ListView item缓存机制：为了使得性能更优，ListView会缓存行item(某行对应的View)。ListView通过adapter的getView函数获得每行的item。滑动过程中，
如果某行item已经滑出屏幕，若该item不在缓存内，则put进缓存，否则更新缓存；
获取滑入屏幕的行item之前会先判断缓存中是否有可用的item，如果有，做为convertView参数传递给adapter的getView。
## 解决方法：
给每个View添加一个tag，加载之前先判断这个view有没有tag，如果有表示这个view是复用的之前就清除掉这个view的图片，重新加载新的图片

参见http://www.trinea.cn/android/android-listview-display-error-image-when-scroll/
