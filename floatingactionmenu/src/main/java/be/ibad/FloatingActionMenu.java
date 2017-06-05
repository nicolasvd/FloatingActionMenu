package be.ibad;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import be.ibad.floatingactionmenu.R;
import be.ibad.listener.OnMenuItemClickListener;
import be.ibad.listener.OnMenuToggleListener;

@CoordinatorLayout.DefaultBehavior(FloatingActionMenu.Behavior.class)
public class FloatingActionMenu extends ViewGroup {

    private static final TimeInterpolator DEFAULT_OPEN_INTERPOLATOR = new OvershootInterpolator();
    private static final TimeInterpolator DEFAULT_CLOSE_INTERPOLATOR = new AnticipateInterpolator();

    private FloatingActionButton menuButton;
    private List<FloatingActionButton> menuItems;
    private List<View> menuLabels;
    private List<ChildAnimator> itemAnimators;
    private View backgroundView;

    private Animator openOverlay;
    private Animator closeOverlay;

    private List<OnMenuItemClickListener> clickListeners;
    private List<OnMenuToggleListener> openListeners;

    private boolean isOpen;
    private boolean isAnimating;
    private boolean displayLabels;
    private boolean isCloseOnTouchOutside = true;
    private long actionsDuration;
    private int menuButtonBackground;
    private int menuButtonRipple;
    private int menuButtonSrc;
    @ColorInt
    private int overlayBackground;
    private int buttonSpacing;
    private int maxButtonWidth;
    @ColorInt
    private int labelBackgroundColor;
    @ColorInt
    private int labelTextColor;
    private int menuMarginEnd;
    private int menuMarginBottom;
    private int overlayDuration;
    private int labelMarginEnd;
    private float labelTextSize;
    private ObjectAnimator collapseIconAnimator;
    GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return isCloseOnTouchOutside && isOpened();
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            close();
            return true;
        }
    });
    private OnClickListener onItemClickListener = new OnClickListener() {
        @Override
        public void onClick(final View view) {
            if (view instanceof FloatingActionButton) {
                int i = menuItems.indexOf(view);
                triggerClickListeners(i, (FloatingActionButton) view);
            } else if (view != backgroundView) {
                int i = menuLabels.indexOf(view);
                triggerClickListeners(i, menuItems.get(i));
            }
            close();
        }
    };
    private ObjectAnimator expandIconAnimator;

    public FloatingActionMenu(Context context) {
        this(context, null, 0);
    }

    public FloatingActionMenu(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatingActionMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray attributes = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.FloatingActionMenu, 0, 0);
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
        int colorAccent = typedValue.data;

        try {
            overlayBackground = attributes
                    .getColor(R.styleable.FloatingActionMenu_overlay_color, Color.parseColor("#7F2a3441"));
            buttonSpacing = attributes
                    .getDimensionPixelSize(R.styleable.FloatingActionMenu_item_spacing, dpToPx(context, 4f));
            menuButtonBackground = attributes.getColor(R.styleable.FloatingActionMenu_base_background, colorAccent);
            menuButtonRipple = attributes
                    .getColor(R.styleable.FloatingActionMenu_base_ripple, Color.parseColor("#66ffffff"));
            menuButtonSrc = attributes
                    .getResourceId(R.styleable.FloatingActionMenu_base_src, R.drawable.ic_positive);
            menuMarginEnd = attributes
                    .getDimensionPixelSize(R.styleable.FloatingActionMenu_base_marginEnd, dpToPx(context, 8f));
            menuMarginBottom = attributes
                    .getDimensionPixelSize(R.styleable.FloatingActionMenu_base_marginBottom, dpToPx(context, 8f));
            overlayDuration = attributes
                    .getInteger(R.styleable.FloatingActionMenu_overlay_duration, getResources().getInteger(android.R.integer.config_longAnimTime));
            actionsDuration = attributes
                    .getInteger(R.styleable.FloatingActionMenu_actions_duration, getResources().getInteger(android.R.integer.config_shortAnimTime));
            labelBackgroundColor = attributes
                    .getColor(R.styleable.FloatingActionMenu_label_background, ContextCompat.getColor(getContext(), android.R.color.white));
            labelTextColor = attributes
                    .getColor(R.styleable.FloatingActionMenu_label_fontColor, Color.BLACK);
            labelTextSize = attributes
                    .getFloat(R.styleable.FloatingActionMenu_label_fontSize, 12f);
            displayLabels = attributes
                    .getBoolean(R.styleable.FloatingActionMenu_enable_labels, true);
            labelMarginEnd = attributes
                    .getDimensionPixelSize(R.styleable.FloatingActionMenu_label_marginEnd, dpToPx(context, 4f));
        } finally {
            attributes.recycle();
        }

        menuItems = new ArrayList<>();
        menuLabels = new ArrayList<>();
        itemAnimators = new ArrayList<>();
        clickListeners = new ArrayList<>();
        openListeners = new ArrayList<>();

        menuButton = new FloatingActionButton(getContext());
        menuButton.setSize(FloatingActionButton.SIZE_AUTO);
        menuButton.setBackgroundTintList(ColorStateList.valueOf(menuButtonBackground));
        menuButton.setRippleColor(menuButtonRipple);
        menuButton.setImageResource(menuButtonSrc);
        menuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });

        backgroundView = new View(getContext());
        backgroundView.setBackgroundColor(overlayBackground);
        backgroundView.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                collapseIconAnimator = ObjectAnimator.ofFloat(menuButton, "rotation", 135f, 0f).setDuration(actionsDuration);
                expandIconAnimator = ObjectAnimator.ofFloat(menuButton, "rotation", 0f, 135f).setDuration(actionsDuration);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {

            }
        });

        addViewInLayout(menuButton, -1, new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        addView(backgroundView);
    }

    static int dpToPx(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * scale);
    }

    @Override
    protected void onFinishInflate() {
        bringChildToFront(menuButton);
        super.onFinishInflate();
    }

    @Override
    public void addView(@NonNull View child, int index, LayoutParams params) {
        super.addView(child, index, params);
        if (child instanceof FloatingActionButton) {
            child.setLayoutParams(params);
            addMenuItem((FloatingActionButton) child);
        }
    }

    public void addMenuItem(FloatingActionButton item) {
        menuItems.add(item);

        if (displayLabels) {
            buildLabelButton(item);
        }

        itemAnimators.add(new ChildAnimator(item));
        item.setOnClickListener(onItemClickListener);
    }

    private void buildLabelButton(FloatingActionButton item) {

        View label = View.inflate(getContext(), R.layout.label_layout, null);

        label.setBackgroundResource(R.drawable.label_background);
        label.getBackground().mutate().setColorFilter(labelBackgroundColor, PorterDuff.Mode.SRC_IN);

        TextView textView = (TextView) label.findViewById(R.id.label_text);
        textView.setText(item.getContentDescription());
        textView.setTextColor(labelTextColor);
        textView.setTextSize(labelTextSize);

        addView(label);

        menuLabels.add(label);
        item.setTag(label);

        label.setOnClickListener(onItemClickListener);
    }

    public void toggle() {
        if (!isOpen) {
            open();
        } else {
            close();
        }
    }

    public void open() {
        startOpenAnimator();
        isOpen = true;

        triggerOpenListeners(true);
    }

    public void close() {
        startCloseAnimator();
        isOpen = false;

        triggerOpenListeners(false);
    }

    private void triggerOpenListeners(boolean state) {
        if (openListeners.size() > 0) {
            for (OnMenuToggleListener listener : openListeners) {
                listener.onMenuToggle(state);
            }
        }
    }

    private void triggerClickListeners(int index, FloatingActionButton button) {
        if (clickListeners.size() > 0) {
            for (OnMenuItemClickListener listener : clickListeners) {
                listener.onMenuItemClick(this, index, button);
            }
        }
    }

    protected void startCloseAnimator() {
        AnimatorSet closeSet = new AnimatorSet();
        closeSet.play(getCloseOverlayAnimator()).with(collapseIconAnimator);
        closeSet.setInterpolator(DEFAULT_CLOSE_INTERPOLATOR);
        closeSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                isAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isAnimating = false;
            }
        });
        closeSet.start();

        for (ChildAnimator anim : itemAnimators) {
            anim.getCloseAnimatorSet().start();
        }
    }

    protected void startOpenAnimator() {
        AnimatorSet openSet = new AnimatorSet();
        openSet.play(getOpenOverlayAnimator()).with(expandIconAnimator);
        openSet.setInterpolator(DEFAULT_OPEN_INTERPOLATOR);
        openSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                isAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isAnimating = false;
            }
        });
        openSet.start();

        for (ChildAnimator anim : itemAnimators) {
            anim.getOpenAnimatorSet().start();
        }
    }

    private Animator getCloseOverlayAnimator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WindowManager manager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int radius = menuButton.getHeight() / 2;

            closeOverlay = ViewAnimationUtils.createCircularReveal(backgroundView,
                    menuButton.getLeft() + radius, menuButton.getTop() + radius,
                    Math.max(size.x, size.y), radius);

        } else {
            if (closeOverlay == null) {
                closeOverlay = ObjectAnimator.ofFloat(backgroundView, "alpha", 1f, 0f);
            }
        }
        closeOverlay.setDuration(overlayDuration);
        closeOverlay.setInterpolator(new AccelerateDecelerateInterpolator());
        closeOverlay.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                backgroundView.setVisibility(GONE);
            }
        });
        return closeOverlay;
    }

    private Animator getOpenOverlayAnimator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WindowManager manager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int radius = menuButton.getHeight() / 2;

            openOverlay = ViewAnimationUtils.createCircularReveal(backgroundView,
                    menuButton.getLeft() + radius, menuButton.getTop() + radius, radius,
                    Math.max(size.x, size.y));
        } else {
            if (openOverlay == null) {
                openOverlay = ObjectAnimator.ofFloat(backgroundView, "alpha", 0f, 1f);
            }
        }
        openOverlay.setDuration(overlayDuration);
        openOverlay.setInterpolator(new AccelerateDecelerateInterpolator());
        openOverlay.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                backgroundView.setVisibility(VISIBLE);
            }
        });
        return openOverlay;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width;
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height;
        final int count = getChildCount();
        maxButtonWidth = 0;

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
        }

        for (int i = 0; i < menuItems.size(); i++) {
            FloatingActionButton fab = menuItems.get(i);
            if (menuLabels.size() > 0) {
                View label = menuLabels.get(i);
                maxButtonWidth = Math.max(maxButtonWidth, label.getMeasuredWidth() + fab.getMeasuredWidth()
                        + ViewCompat.getPaddingEnd(fab) + ViewCompat.getPaddingStart(fab));

            }
        }

        maxButtonWidth = Math.max(menuButton.getMeasuredWidth(), maxButtonWidth);

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else {
            width = maxButtonWidth;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else {
            int heightSum = 0;
            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);
                heightSum += child.getMeasuredHeight() + child.getPaddingBottom();
            }
            height = heightSum;
        }

        setMeasuredDimension(resolveSize(width, widthMeasureSpec),
                resolveSize(height, heightMeasureSpec));
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (isCloseOnTouchOutside) {
            return gestureDetector.onTouchEvent(event);
        } else {
            return super.onTouchEvent(event);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed) {
            backgroundView.layout(l, 0, r, b);

            int buttonsHorizontalCenter = r - l - menuButton.getMeasuredWidth() / 2 - getPaddingRight() - menuMarginEnd;
            int menuButtonTop = b - t - menuButton.getMeasuredHeight() - getPaddingBottom() - menuMarginBottom;
            int menuButtonLeft = buttonsHorizontalCenter - menuButton.getMeasuredWidth() / 2;

            menuButton.layout(menuButtonLeft, menuButtonTop,
                    menuButtonLeft + menuButton.getMeasuredWidth(),
                    menuButtonTop + menuButton.getMeasuredHeight());

            int nextY = menuButtonTop;

            int itemCount = menuItems.size();
            for (int i = 0; i < itemCount; i++) {
                FloatingActionButton item = menuItems.get(i);

                if (item.getVisibility() != GONE) {

                    int childX = buttonsHorizontalCenter - item.getMeasuredWidth() / 2;
                    int childY = nextY - item.getMeasuredHeight() - buttonSpacing;

                    item.layout(childX, childY, childX + item.getMeasuredWidth(), childY + item.getMeasuredHeight());

                    View label = (View) item.getTag();
                    if (label != null) {
                        int labelsOffset = item.getMeasuredWidth() / 2 + labelMarginEnd;
                        int labelXEnd = buttonsHorizontalCenter - labelsOffset;
                        int labelXStart = labelXEnd - label.getMeasuredWidth();
                        int labelTop = childY + (item.getMeasuredHeight() - label.getMeasuredHeight()) / 2;

                        label.layout(labelXStart, labelTop, labelXEnd, labelTop + label.getMeasuredHeight());

                        if (!isAnimating) {
                            if (!isOpen) {
                                label.setTranslationX(item.getLeft() - label.getLeft());
                                label.setAlpha(0f);
                                label.setVisibility(INVISIBLE);
                            }
                        }
                    }

                    nextY = childY;

                    if (!isAnimating) {
                        if (!isOpen) {
                            item.setTranslationY(menuButton.getTop() - item.getTop());
                            item.setVisibility(INVISIBLE);
                            backgroundView.setVisibility(INVISIBLE);
                        }
                    }
                }
            }

            if (!isAnimating && getBackground() != null) {
                if (!isOpen) {
                    getBackground().setAlpha(0);
                } else {
                    getBackground().setAlpha(0xff);
                }
            }
        }
    }

    public boolean isOpened() {
        return isOpen;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("instanceState", super.onSaveInstanceState());
        bundle.putBoolean("openState", isOpen);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            isOpen = bundle.getBoolean("openState");
            state = bundle.getParcelable("instanceState");
        }

        super.onRestoreInstanceState(state);
    }

    public void addOnMenuItemClickListener(OnMenuItemClickListener listener) {
        this.clickListeners.add(listener);
    }

    public void addOnMenuToggleListener(OnMenuToggleListener listener) {
        this.openListeners.add(listener);
    }

    public boolean removeOnMenuItemClickListener(OnMenuItemClickListener listener) {
        return this.clickListeners.remove(listener);
    }

    public boolean removeOnMenuToggleListener(OnMenuToggleListener listener) {
        return this.openListeners.remove(listener);
    }

    /**
     * Behavior designed for use with {@link FloatingActionMenu} instances. It's main function
     * is to move all {@link FloatingActionButton}s views inside {@link FloatingActionMenu} so
     * that any displayed {@link Snackbar}s do not cover them.
     */
    public static class Behavior extends CoordinatorLayout.Behavior<FloatingActionMenu> {

        /**
         * Default constructor for instantiating Behaviors.
         */
        public Behavior() {
        }

        public Behavior(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionMenu child, View dependency) {
            return dependency instanceof Snackbar.SnackbarLayout;
        }

        @Override
        public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionMenu child, View dependency) {
            float translationY = Math.min(0, dependency.getTranslationY() - dependency.getHeight());
            child.setTranslationY(translationY);
            return true;
        }
    }

    private class ChildAnimator {
        final View mLabel;
        final View mChild;

        ChildAnimator(final View view) {
            mLabel = (View) view.getTag();
            this.mChild = view;
        }

        AnimatorSet getCloseAnimatorSet() {
            AnimatorSet closeAnimatorSet = new AnimatorSet();

            ObjectAnimator closeLabelAlphaAnimator = ObjectAnimator.ofFloat(mLabel, "alpha", 1f, 0f);
            ObjectAnimator closeLabelTranslateAnimator = ObjectAnimator.ofFloat(mLabel, "translationX", mChild.getLeft() - mLabel.getLeft());
            ObjectAnimator closeChildTranslateAnimator = ObjectAnimator.ofFloat(mChild, "translationY", menuButton.getTop() - mChild.getTop());
            closeChildTranslateAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mChild.setVisibility(GONE);
                    if (displayLabels) {
                        mLabel.setVisibility(GONE);
                    }
                }
            });
            closeAnimatorSet.setInterpolator(DEFAULT_CLOSE_INTERPOLATOR);
            closeAnimatorSet
                    .play(closeLabelAlphaAnimator)
                    .with(closeLabelTranslateAnimator)
                    .before(closeChildTranslateAnimator);

            return closeAnimatorSet;
        }

        AnimatorSet getOpenAnimatorSet() {
            AnimatorSet openAnimatorSet = new AnimatorSet();
            ObjectAnimator openLabelAlphaAnimator = ObjectAnimator.ofFloat(mLabel, "alpha", 0f, 1f);
            ObjectAnimator openLabelTranslateAnimator = ObjectAnimator.ofFloat(mLabel, "translationX", 0f);
            ObjectAnimator openChildTranslateAnimator = ObjectAnimator.ofFloat(mChild, "translationY", 0f);
            openChildTranslateAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    mChild.setVisibility(VISIBLE);
                    if (displayLabels) {
                        mLabel.setVisibility(VISIBLE);
                    }
                }
            });
            openAnimatorSet.setInterpolator(DEFAULT_OPEN_INTERPOLATOR);
            openAnimatorSet
                    .play(openLabelAlphaAnimator)
                    .with(openLabelTranslateAnimator)
                    .after(openChildTranslateAnimator);

            return openAnimatorSet;
        }
    }
}
