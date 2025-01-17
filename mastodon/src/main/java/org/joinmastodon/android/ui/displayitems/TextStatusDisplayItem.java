package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.LinkedTextView;

import java.util.Locale;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.MovieDrawable;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class TextStatusDisplayItem extends StatusDisplayItem{
	private CharSequence text;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	private CharSequence translatedText;
	private CustomEmojiHelper translationEmojiHelper=new CustomEmojiHelper();
	public boolean textSelectable;
	public boolean reduceTopPadding;
	public final Status status;

	public TextStatusDisplayItem(String parentID, CharSequence text, BaseStatusListFragment parentFragment, Status status){
		super(parentID, parentFragment);
		this.text=text;
		this.status=status;
		emojiHelper.setText(text);
	}

	@Override
	public Type getType(){
		return Type.TEXT;
	}

	@Override
	public int getImageCount(){
		return getCurrentEmojiHelper().getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return getCurrentEmojiHelper().getImageRequest(index);
	}

	public void setTranslatedText(String text){
		Status statusForContent=status.getContentStatus();
		translatedText=HtmlParser.parse(text, statusForContent.emojis, statusForContent.mentions, statusForContent.tags, parentFragment.getAccountID());
		translationEmojiHelper.setText(translatedText);
	}

	private CustomEmojiHelper getCurrentEmojiHelper(){
		return status.translationState==Status.TranslationState.SHOWN ? translationEmojiHelper : emojiHelper;
	}

	public static class Holder extends StatusDisplayItem.Holder<TextStatusDisplayItem> implements ImageLoaderViewHolder{
		private final LinkedTextView text;
		private final ViewStub translationFooterStub;
		private View translationFooter;
		private TextView translationInfo;
		private Button translationShowOriginal;
		private ProgressBar translationProgress;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_text, parent);
			text=findViewById(R.id.text);
			translationFooterStub=findViewById(R.id.translation_info);
		}

		@Override
		public void onBind(TextStatusDisplayItem item){
			if(item.status.translationState==Status.TranslationState.SHOWN){
				if(item.translatedText==null){
					item.setTranslatedText(item.status.translation.content);
				}
				text.setText(item.translatedText);
			}else{
				text.setText(item.text);
			}
			text.setTextIsSelectable(item.textSelectable);
			text.setInvalidateOnEveryFrame(false);
			itemView.setClickable(false);
			text.setPadding(text.getPaddingLeft(), item.reduceTopPadding ? V.dp(8) : V.dp(16), text.getPaddingRight(), text.getPaddingBottom());
			text.setTextColor(UiUtils.getThemeColor(text.getContext(), item.inset ? R.attr.colorM3OnSurfaceVariant : R.attr.colorM3OnSurface));
			updateTranslation(false);
		}

		@Override
		public void setImage(int index, Drawable image){
			getEmojiHelper().setImageDrawable(index, image);
			text.invalidate();
			if(image instanceof Animatable){
				((Animatable) image).start();
				if(image instanceof MovieDrawable)
					text.setInvalidateOnEveryFrame(true);
			}
		}

		@Override
		public void clearImage(int index){
			getEmojiHelper().setImageDrawable(index, null);
			text.invalidate();
		}

		private CustomEmojiHelper getEmojiHelper(){
			return item.emojiHelper;
		}

		public void updateTranslation(boolean updateText){
			if(item.status==null)
				return;
			if(item.status.translationState==Status.TranslationState.HIDDEN){
				if(translationFooter!=null)
					translationFooter.setVisibility(View.GONE);
				if(updateText){
					text.setText(item.text);
				}
			}else{
				if(translationFooter==null){
					translationFooter=translationFooterStub.inflate();
					translationInfo=findViewById(R.id.translation_info_text);
					translationShowOriginal=findViewById(R.id.translation_show_original);
					translationProgress=findViewById(R.id.translation_progress);
					translationShowOriginal.setOnClickListener(v->item.parentFragment.togglePostTranslation(item.status, item.parentID));
				}else{
					translationFooter.setVisibility(View.VISIBLE);
				}
				if(item.status.translationState==Status.TranslationState.SHOWN){
					translationProgress.setVisibility(View.GONE);
					translationInfo.setVisibility(View.VISIBLE);
					translationShowOriginal.setVisibility(View.VISIBLE);
					translationInfo.setText(translationInfo.getContext().getString(R.string.post_translated, Locale.forLanguageTag(item.status.translation.detectedSourceLanguage).getDisplayLanguage(), item.status.translation.provider));
					if(updateText){
						if(item.translatedText==null){
							item.setTranslatedText(item.status.translation.content);
						}
						text.setText(item.translatedText);
					}
				}else{ // LOADING
					translationProgress.setVisibility(View.VISIBLE);
					translationInfo.setVisibility(View.INVISIBLE);
					translationShowOriginal.setVisibility(View.INVISIBLE);
				}
			}
		}
	}
}
