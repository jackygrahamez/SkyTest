package com.skytree.epubtest;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import com.skytree.epubtest.R;
import com.skytree.epub.Book;
import com.skytree.epub.CacheListener;
import com.skytree.epub.ClickListener;
import com.skytree.epub.ContentListener;
import com.skytree.epub.FixedControl;
import com.skytree.epub.MediaOverlayListener;
import com.skytree.epub.PageInformation;
import com.skytree.epub.PageMovedListener;
import com.skytree.epub.PageTransition;
import com.skytree.epub.Parallel;
import com.skytree.epub.Setting;
import com.skytree.epubtest.ContentHandler;
import com.skytree.epubtest.SkyLayout;
import com.skytree.epubtest.BookViewActivity.ImageButtonHighlighterOnTouchListener;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;

public class MagazineActivity extends Activity {
	RelativeLayout ePubView,topView;
	FixedControl fv;
	SkyPieView pieView;
	View pieBack;

	SkyLayout mediaBox;
 	ImageButton playAndPauseButton;
 	ImageButton stopButton;
 	ImageButton prevButton;
 	ImageButton nextButton;

	
	Parallel currentParallel;
	boolean autoStartPlayingWhenNewPagesLoaded = true;  
	boolean autoMovePageWhenParallesFinished = true;	
	boolean isAutoPlaying = true;						
	int bookCode;
	double pagePositionInBook;
	boolean isCaching = false;
	
 	SkySetting setting;
 	SkyDatabase sd;
 	boolean isRotationLocked;
	
	private void debug(String msg) {
		Log.w("EPub",msg);
	}
	
	
    public void onCreate(Bundle savedInstanceState) {
    	sd = new SkyDatabase(this);
		setting = sd.fetchSetting();
    	
        String fileName = new String();
		Bundle bundle = getIntent().getExtras();
		fileName = bundle.getString("BOOKNAME");
		bookCode = bundle.getInt("BOOKCODE");
		pagePositionInBook = bundle.getDouble("POSITION");
		
		int spread = bundle.getInt("SPREAD");
		int orientation = bundle.getInt("ORIENTATION");		
		
		super.onCreate(savedInstanceState); 		
		
		ePubView = new RelativeLayout(this);
		RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT,RelativeLayout.LayoutParams.FILL_PARENT);
		ePubView.setLayoutParams(rlp);
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT); // width,height        
//		int maxCachesForCaptures = 10; // for low spec device, set this value below 25, default 25
//		fv = new FixedControl(this,spread,orientation,maxCachesForCaptures); 
		fv = new FixedControl(this,spread,orientation); 			// set the spread by the spread property in book.  
//		fv = new FixedControl(this,Book.SpreadNone,orientation);	// set only single page for both portrait and landscape. 
//		fv = new FixedControl(this,Book.SpreadAuto,orientation);	// engine will change spread mode automatically.
		Bitmap pagesStack = 	BitmapFactory.decodeFile(SkySetting.getStorageDirectory()+"/images/PagesStack.png");
		Bitmap pagesCenter = 	BitmapFactory.decodeFile(SkySetting.getStorageDirectory()+"/images/PagesCenter.png");		
		fv.setPagesCenterImage(pagesCenter);
//		fv.setPagesStackImage(pagesStack);
		CacheDelegate ch = new CacheDelegate();
		fv.setCacheListener(ch);
		fv.setBaseDirectory(SkySetting.getStorageDirectory() + "/books");
        fv.setBookName(fileName);
		fv.setContentListener(new ContentHandler());
		fv.setContentListenerForCache(new ContentHandler());
        fv.setLayoutParams(params);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        params.width =  LayoutParams.FILL_PARENT;	// 400;
        params.height = LayoutParams.FILL_PARENT;	// 600;
        ClickDelegate cd = new ClickDelegate();
        fv.setClickListener(cd);
        PageMovedDelegate pd = new PageMovedDelegate();
        fv.setPageMovedListener(pd);
//        fv.setTimeForRendering(1500);
        fv.setTimeForRendering(500);
        fv.setCurlQuality(0.75f);
        fv.setAutoScroll(false);
        fv.setNavigationAreaWidthRatio(0.2f);
        fv.setMediaOverlayListener(new MediaOverlayDelegate());
        fv.setSequenceBasedForMediaOverlay(false);
//        fv.setMaxCachesForLoad(15);	// set max number of pages to be cached in memory for fast page transition. 

        // If you want to get the license key for commercial or non commercial use, please don't hesitate to email us. (skytree21@gmail.com). 
		// Without the proper license key, watermark message(eg.'unlicensed') may be shown in background. 
		fv.setLicenseKey("0000-0000-0000-0000");	

		int transitionType = bundle.getInt("transitionType");
		if (transitionType==0) {
			fv.setPageTransition(PageTransition.None);
		}else if (transitionType==1) {
			fv.setPageTransition(PageTransition.Slide);
		}else if (transitionType==2) {
			fv.setPageTransition(PageTransition.Curl);
		}
		
		// set the minimum speed for pageTransition 
		// to avoid the conflict between dragging object inside page and swiping for turning page.
		// if you don't need to drag objects inside page, set this value 0.1f for smooth curling.	
		// The value of 1.0f or above can be useful for  dragging object inside page.  
		fv.setSwipeSpeedForPageTransition(0.1f);
		
		isRotationLocked = setting.lockRotation;
		
        ePubView.addView(fv);

        this.makeMediaBox();
        int startPageIndex = (int)pagePositionInBook;
        fv.setStartPageIndex(startPageIndex); 
        setContentView(ePubView);
        fv.setImmersiveMode(true);
        SkyUtility.makeFullscreen(this);
        
		topView = new RelativeLayout(this);
		RelativeLayout.LayoutParams tlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT,RelativeLayout.LayoutParams.FILL_PARENT);
		topView.setLayoutParams(tlp);
		topView.setVisibility(View.INVISIBLE);
		topView.setVisibility(View.GONE);
		topView.setOnTouchListener(new OnTouchListener() {
		    @Override
		    public boolean onTouch(View v, MotionEvent event) {
		        return true;
		    }
		});
		
		ePubView.addView(topView);	
		
		
		pieView = new SkyPieView(this);
		pieView.setId(7080);
		pieView.isHidden = false;
		pieView.setVisibility(View.VISIBLE);
		int pw = getPS(300);
		int ph = pw;
		int px = (this.getWidth()-pw)/2;
		int py = (this.getHeight()-ph)/2;
		SkyUtility.setFrame(pieView,px,py,pw,ph);			

		pieBack = new View(this);
		int bw = getPS(180);
		int bh = bw;
		int bx = (this.getWidth()-bw)/2;
		int by = (this.getHeight()-bh)/2;

		SkyUtility.setFrame(pieBack,bx,by,bw,bh);
		pieBack.setBackgroundColor(Color.argb(200, 80, 80, 80));
		topView.addView(pieBack);		
		topView.addView(pieView);
		
		this.makeIndicator();
		
		if (fv.isDebugging()) {
			showToast("In Debugging Mode, Caching Task Not Started.!!!!");
		}
    }    
    
	private OnClickListener listener=new OnClickListener(){
		public void onClick(View arg) {			
			if (arg.getId() == 8080) {
				playPrev();
			} else if (arg.getId() == 8081) {
				playAndPause();				
			} else if (arg.getId() == 8082) {
				stopPlaying();		
			} else if (arg.getId() == 8083) {
				playNext();
			} else if (arg.getId() == 8084) {
				finish();				
			}	    
		}
	};
	
	class MediaOverlayDelegate implements MediaOverlayListener {
		@Override
		public void onParallelStarted(Parallel parallel) {
			// TODO Auto-generated method stub
			fv.changeElementColor("#FFFF00",parallel.hash,parallel.pageIndex);
			currentParallel = parallel;			
		}

		@Override
		public void onParallelEnded(Parallel parallel) {
			// TODO Auto-generated method stub
			fv.restoreElementColor();			
		}

		@Override
		public void onParallelsEnded() {
			// TODO Auto-generated method stub
		    fv.restoreElementColor();
		    if (autoStartPlayingWhenNewPagesLoaded) isAutoPlaying = true;
		    if (autoMovePageWhenParallesFinished) {
		        if (!isCaching) fv.gotoNextPage();
		    }
		}		
	}
	
	void playAndPause() {
		if (fv.isPlayingPaused()) {
	        if (!fv.isPlayingStarted()) {
	            fv.playFirstParallel();	        	
	            if (autoStartPlayingWhenNewPagesLoaded) isAutoPlaying = true;
	        }else {
	            fv.resumePlayingParallel();	        	
	            if (autoStartPlayingWhenNewPagesLoaded) isAutoPlaying = true;
	        }        
	    
	    }else {	        
	        fv.pausePlayingParallel();
	        if (autoStartPlayingWhenNewPagesLoaded) isAutoPlaying = false;
	    }
		this.changePlayAndPauseButton();
	}
	
	void stopPlaying() {
	    fv.stopPlayingParallel();
	    fv.restoreElementColor();
	    if (autoStartPlayingWhenNewPagesLoaded) isAutoPlaying = false;
	    this.changePlayAndPauseButton();
	}
	
	void playPrev() {
	    fv.restoreElementColor();
	    if (currentParallel.parallelIndex==0) {
	        if (autoMovePageWhenParallesFinished) fv.gotoPrevPage();
	    }else {
	        fv.playPrevParallel();
	    }
	}

	void playNext() {
	    fv.restoreElementColor();
		fv.playNextParallel();
	}
	
	public void onStop() {
		super.onStop();
//		debug("onStop");
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		sd.updatePosition(bookCode, pagePositionInBook);
		sd.updateSetting(setting);
		
		fv.stopPlayingParallel();
		fv.restoreElementColor();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		fv.playFirstParallel();
	}

	public void onDestory() {
		super.onDestroy();
		debug("onDestory");		
	}
	
	@Override
	 public void onBackPressed() {
		finish();
		return;
	 }

	
	public void makeMediaBox() {
		mediaBox = new SkyLayout(this);
		setFrame(mediaBox, 100, 200, ps(270),ps(50));
		
		int bs = ps(32);
		int sb = 15;
		prevButton = this.makeImageButton(9898, R.drawable.prev2x, bs,bs);
		setLocation(prevButton,ps(10),ps(5));
		prevButton.setId(8080);
		prevButton.setOnClickListener(listener);
		playAndPauseButton = this.makeImageButton(9898, R.drawable.pause2x, bs,bs);
		setLocation(playAndPauseButton,ps(sb)+bs+ps(10),ps(5));
		playAndPauseButton.setId(8081);
		playAndPauseButton.setOnClickListener(listener);
		stopButton = this.makeImageButton(9898, R.drawable.stop2x, bs,bs);
		setLocation(stopButton,(ps(sb)+bs)*2,ps(5));
		stopButton.setId(8082);
		stopButton.setOnClickListener(listener);
		nextButton = this.makeImageButton(9898, R.drawable.next2x, bs,bs);
		setLocation(nextButton,(ps(sb)+bs)*3,ps(5));
		nextButton.setId(8083);
		nextButton.setOnClickListener(listener);
		
		mediaBox.setVisibility(View.INVISIBLE);
		mediaBox.setVisibility(View.GONE);		
		
		mediaBox.addView(prevButton);
		mediaBox.addView(playAndPauseButton);
		mediaBox.addView(stopButton);
		mediaBox.addView(nextButton);
		this.ePubView.addView(mediaBox);		
	}

	
	public boolean isPortrait() {
		int orientation = getResources().getConfiguration().orientation;
		if (orientation==Configuration.ORIENTATION_PORTRAIT) return true;
		else return false;	
	}
	
	public void hideMediaBox() {
		if (mediaBox!=null) {
			mediaBox.setVisibility(View.INVISIBLE);
			mediaBox.setVisibility(View.GONE);
		}
	}
	
	public void showMediaBox() {
		if (this.isPortrait()) {
			setLocation(mediaBox,ps(50), ps(120));
		}else {
			setLocation(mediaBox, ps(110), ps(20));
		}
		mediaBox.setVisibility(View.VISIBLE);
	}
	
	private void changePlayAndPauseButton() {
		Drawable icon;
		int imageId;
		if (!fv.isPlayingStarted() || fv.isPlayingPaused()) {
			imageId = R.drawable.play2x;
		}else {
			imageId = R.drawable.pause2x;	
		}
		
		int bs = ps(32);
		icon = getResources().getDrawable(imageId);
		icon.setBounds(0,0,bs,bs);
		Bitmap iconBitmap = ((BitmapDrawable)icon).getBitmap();
		Bitmap bitmapResized = Bitmap.createScaledBitmap(iconBitmap, bs, bs, false);
		playAndPauseButton.setImageBitmap(bitmapResized);
	}

	
	class PageMovedDelegate implements PageMovedListener {
		public void onPageMoved(PageInformation pi) {
			pagePositionInBook = pi.pageIndex;
//			String msg = String.format("pn:%d/tn:%d ps:%f",pi.pageIndex,pi.numberOfPagesInChapter,pi.pagePositionInBook);
	        if (fv.isMediaOverlayAvailable()) {
	        	showMediaBox();
	            if (isAutoPlaying) {
	                fv.playFirstParallel();
	            }
	        }else {
	        	hideMediaBox();
	        }
		}
		
		public void onChapterLoaded(int chapterIndex) {
			// do nothing in FixedLayout. 
		}
	}
	
	public void setFrame(View view,int dx, int dy, int width, int height) {
		RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT); // width,height
		param.leftMargin = dx;
		param.topMargin =  dy;
		param.width = width;
		param.height = height;
		view.setLayoutParams(param);	
	}
	
	public void setLocation(View view,int px, int py) {
		RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT); // width,height
		param.leftMargin = px;
		param.topMargin =  py;
		view.setLayoutParams(param);
	}
	
	public int getDensityDPI() {
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		int density = metrics.densityDpi;
		return density;
	}
	
	public int getPS(float dip) {
		float densityDPI = this.getDensityDPI();
		int px = (int)(dip*(densityDPI/240));
		return px;		
//		int px = (int)(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics()));
//		return px;
	}
	
	public int getPXFromLeft(float dip) {
		int ps = this.getPS(dip);
		return ps;		
	}
	
	public int getPXFromRight(float dip) {
		int ps = this.getPS(dip);
		int ms = this.getWidth()-ps;
		return ms;
	}
	
	public int getPYFromTop(float dip) {
		int ps = this.getPS(dip);
		return ps;
	}
	
	public int getPYFromBottom(float dip) {
		int ps = this.getPS(dip);
		int ms = this.getHeight()-ps;
		return ms;
	}
	
	public int pxl(float dp) {
		return this.getPXFromLeft(dp);		
	}
	
	public int pxr(float dp) {
		return this.getPXFromRight(dp);
	}
	
	public int pyt(float dp) {
		return this.getPYFromTop(dp);
	}
	
	public int pyb(float dp) {
		return this.getPYFromBottom(dp);
	}
	
	public int ps(float dp) {
		return this.getPS(dp);
	}
	
	public int pw(float sdp) {
		int ps = this.getPS(sdp*2);
		int ms = this.getWidth()-ps;
		return ms;
	}
	
	public int cx(float dp) {
		int ps = this.getPS(dp);
		int ms = this.getWidth()/2-ps/2;
		return ms;		
	}
	
	public int getWidth() {
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		int width = metrics.widthPixels;
		return width;		
	}
	
	public int getHeight() {
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		int height = metrics.heightPixels;
		return height;			
	}
	
	public ImageButton makeImageButton(int id,String imageName,int width, int height) {
		RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT); // width,height
		Drawable icon;
		ImageButton button = new ImageButton(this);	
		button.setId(id);
		button.setOnClickListener(listener);
		button.setBackgroundColor(Color.TRANSPARENT);
		icon = this.getDrawableFromAssets(imageName);
		icon.setBounds(0,0,width,height);
		Bitmap iconBitmap = ((BitmapDrawable)icon).getBitmap();
		Bitmap bitmapResized = Bitmap.createScaledBitmap(iconBitmap, width, height, false);
		button.setImageBitmap(bitmapResized);
		button.setVisibility(View.VISIBLE);
		param.width = 		width;		
		param.height = 		height;
		button.setLayoutParams(param);		
		button.setOnTouchListener(new ImageButtonHighlighterOnTouchListener(button));
		return button;		
	}
	
	public ImageButton makeImageButton(int id,int resId,int width, int height) {
		RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT); // width,height
		Drawable icon;
		ImageButton button = new ImageButton(this);
		button.setAdjustViewBounds(true);
		button.setId(id);
		button.setOnClickListener(listener);
		button.setBackgroundColor(Color.TRANSPARENT);
		icon = getResources().getDrawable(resId);
		icon.setBounds(0,0,width,height);
		
		Bitmap iconBitmap = ((BitmapDrawable)icon).getBitmap();
		Bitmap bitmapResized = Bitmap.createScaledBitmap(iconBitmap, width, height, false);
		button.setImageBitmap(bitmapResized);
		button.setVisibility(View.VISIBLE);
		param.width = 		(int)(width);		
		param.height = 		(int)(height);
		button.setLayoutParams(param);		
		button.setOnTouchListener(new ImageButtonHighlighterOnTouchListener(button));
		return button;		
	}

	
	Drawable getDrawableFromAssets(String name) {
		try {
//			InputStream ims = getResources().getAssets().open(name);
//			Drawable d = Drawable.createFromStream(ims, null);
			Drawable d = Drawable.createFromStream(getAssets().open(name), null);
			return d;
		}catch(Exception e) {
			return null;
		}	
	}
	
	class ImageButtonHighlighterOnTouchListener implements OnTouchListener {
		  final ImageButton button;

		  public ImageButtonHighlighterOnTouchListener(final ImageButton button) {
		    super();
		    this.button = button;
		  }
		  
		  @Override
		  public boolean onTouch(final View view, final MotionEvent motionEvent) {
			  if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
				  button.setColorFilter(Color.argb(155, 220, 220, 220));
			  } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
				  button.setColorFilter(Color.argb(0, 185, 185, 185));
			  }
		    return false;
		  }
	}
	
	private void showToast(String msg) {
		Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		toast.show();
	}
	
 	ProgressBar progressBar;
	
	public void makeIndicator() {
		progressBar = new ProgressBar(this, null,android.R.attr.progressBarStyleSmallInverse);		
		ePubView.addView(progressBar);		
		this.hideIndicator();
	}
	
	public void showIndicator() {
		RelativeLayout.LayoutParams params = 
			    (RelativeLayout.LayoutParams)progressBar.getLayoutParams();		
		params.addRule(RelativeLayout.CENTER_IN_PARENT, -1);
		progressBar.setLayoutParams(params);
		progressBar.setVisibility(View.VISIBLE);
	}
	
	public void showPieView() {
		this.pieView.setVisibility(View.VISIBLE);
		this.pieBack.setVisibility(View.VISIBLE);
	}
	
	public void hidePieView() {
		this.pieView.setVisibility(View.INVISIBLE);
		this.pieBack.setVisibility(View.INVISIBLE);
		this.pieView.setVisibility(View.GONE);
		this.pieBack.setVisibility(View.GONE);
	}
	
	public void hideIndicator() {
		if (progressBar!=null) {
			progressBar.setVisibility(View.INVISIBLE);
			progressBar.setVisibility(View.GONE);
		}
	}	
	
	class CacheDelegate implements CacheListener {
		@Override
		public void onCachingStarted(int numberOfUncached) {
			// TODO Auto-generated method stub
			if (numberOfUncached<=3) return;			
//			showToast("Caching task started.");
//			Setting.debug("Caching task started.");
			topView.setVisibility(View.VISIBLE);
//			fv.setRotationLocked(true);
			hidePieView();
			showIndicator();
			isCaching = true;
		}

		@Override
		public void onCachingFinished(int numberOfCached) {
			// TODO Auto-generated method stub
//			showToast("Caching task ended");
//			Setting.debug("Caching task ended.");
			topView.setVisibility(View.INVISIBLE);
			topView.setVisibility(View.GONE);
//			fv.setRotationLocked(setting.lockRotation);
			hideIndicator();
			isCaching = false; 
		}

		@Override
		public void onCached(int index, String path,double progress) {
			// TODO Auto-generated method stub
			String msg = "Progress "+(int)(progress*100)+" PageIndex "+index+" is cached to "+path;
			Setting.debug(msg);
			pieView.setValue(progress);
			pieView.invalidate();
			hideIndicator();
			showPieView();
//			showToast(msg);			
		}		
	}
}


class ClickDelegate implements ClickListener {
	public void onClick(int x,int y) {
//		Setting.debug("Click detected at "+x+":"+y);
	}
	public void onImageClicked(int x,int y,String src) {}
	public void onLinkClicked(int x,int y,String href) {}
	@Override
	public boolean ignoreLink(int x,int y,String href) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void onLinkForLinearNoClicked(int x, int y, String href) {}
	@Override
	public void onIFrameClicked(int x, int y, String src) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onVideoClicked(int x, int y, String src) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onAudioClicked(int x, int y, String src) {
		// TODO Auto-generated method stub
		
	}
}