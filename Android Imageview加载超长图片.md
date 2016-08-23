通过BitmapRegionDecoder可以选择加载图片的某一区域。
```java
InputStream is = getResources().openRawResource(R.drawable.changtu);
mDecoder = BitmapRegionDecoder.newInstance(is, true);
mRect.set(left, top, right, bottom);
Bitmap bm = mDecoder.decodeRegion(mRect, null);
mView.setImageBitmap(bm);
```
完整代码如下:
```java
public class MainActivity extends Activity implements OnTouchListener {

	private final Rect mRect = new Rect();
	private BitmapRegionDecoder mDecoder;
	private ImageView mView;
	private int left, top, right, bottom;
	int lastY = 0;
	int curY = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mView = new ImageView(this);
		mView.setAdjustViewBounds(true);
		mView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		mView.setScaleType(ScaleType.CENTER);
		mView.setOnTouchListener(this);
		setContentView(mView);

		try {
			InputStream is = getResources().openRawResource(R.drawable.changtu);
			mDecoder = BitmapRegionDecoder.newInstance(is, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		final int action = event.getAction() & MotionEvent.ACTION_MASK;
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			lastY = curY = (int) event.getY();
		case MotionEvent.ACTION_MOVE:
			curY = (int) event.getY();
			int deltaY = curY - lastY;
			setImageRegion(-1 * deltaY);
			lastY = curY;
			break;
		}
		return true;
	}

	private void setImageRegion(int deltaY) {
		final int width = mView.getWidth();
		final int height = mView.getHeight();

		final int imgWidth = mDecoder.getWidth();
		final int imgHeight = mDecoder.getHeight();

		top += deltaY;
		if (top + height >= imgHeight) {
			top = imgHeight - height;
		}
		if (top <= 0) {
			top = 0;
		}

		right = left + width;
		bottom = top + height;

		mRect.set(left, top, right, bottom);
		Bitmap bm = mDecoder.decodeRegion(mRect, null);
		mView.setImageBitmap(bm);
	}

}
```