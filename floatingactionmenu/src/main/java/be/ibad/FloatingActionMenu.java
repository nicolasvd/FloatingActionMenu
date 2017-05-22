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
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
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

    private AnimatorSet openSet = new AnimatorSet();
    private AnimatorSet closeSet = new AnimatorSet();
    private Animator openOverlay;
    private Animator closeOverlay;

    private List<OnMenuItemClickListener> clickListeners;
    private List<OnMenuToggleListener> openListeners;

    private boolean isOpen;
    private boolean isAnimating;
    private boolean displayLabels;
    private boolean isCloseOnTouchOutside = true;
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
    private long actionsDuration;
    private int menuButtonBackground;
    private int menuButtonRipple;
    private int menuButtonSrc;
    private int overlayBackground;
    private int buttonSpacing;
    private int maxButtonWidth;
    private int labelBackground;
    private int labelTextColor;
    private int menuMarginEnd;
    private int menuMarginBottom;
    private int overlayDuration;
    private int labelMarginEnd;
    private float labelTextSize;
    private OnClickListener onItemClickListener = new OnClickListener() {
        @Override
        public void onClick(final View view) {
            closeSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    closeSet.removeListener(this);
                    if (view instanceof FloatingActionButton) {
                        int i = menuItems.indexOf(view);
                        triggerClickListeners(i, (FloatingActionButton) view);
                    } else if (view != backgroundView) {
                        int i = menuLabels.indexOf(view);
                        triggerClickListeners(i, menuItems.get(i));
                    }
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });

            close();
        }
    };

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

        try {
            overlayBackground = attributes
                    .getColor(R.styleable.FloatingActionMenu_overlay_color, Color.parseColor("#7F2a3441"));
            buttonSpacing = attributes
                    .getDimensionPixelSize(R.styleable.FloatingActionMenu_item_spacing, dpToPx(context, 8f));
            menuButtonBackground = attributes
                    .getColor(R.styleable.FloatingActionMenu_base_background, Color.RED);
            menuButtonRipple = attributes
                    .getColor(R.styleable.FloatingActionMenu_base_ripple, Color.parseColor("#66ffffff"));
            menuButtonSrc = attributes
                    .getResourceId(R.styleable.FloatingActionMenu_base_src, R.drawable.ic_positive);
            menuMarginEnd = attributes
                    .getDimensionPixelSize(R.styleable.FloatingActionMenu_base_marginEnd, 0);
            menuMarginBottom = attributes
                    .getDimensionPixelSize(R.styleable.FloatingActionMenu_base_marginBottom, 0);
            overlayDuration = attributes
                    .getInteger(R.styleable.FloatingActionMenu_overlay_duration, 500);
            actionsDuration = attributes
                    .getInteger(R.styleable.FloatingActionMenu_actions_duration, 300);
            labelBackground = attributes
                    .getResourceId(R.styleable.FloatingActionMenu_label_background, R.drawable.label_background);
            labelTextColor = attributes
                    .getColor(R.styleable.FloatingActionMenu_label_fontColor, Color.BLACK);
            labelTextSize = attributes
                    .getFloat(R.styleable.FloatingActionMenu_label_fontSize, 12f);
            displayLabels = attributes
                    .getBoolean(R.styleable.FloatingActionMenu_enable_labels, true);
            labelMarginEnd = attributes
                    .getDimensionPixelSize(R.styleable.FloatingActionMenu_label_marginEnd, 0);
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
                createDefaultIconAnimation();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {

            }
        });

        addViewInLayout(menuButton, -1, new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        addView(backgroundView);
        createOverlayRipple();
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
        label.setBackgroundResource(labelBackground);

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

    private void createOverlayRipple() {
        WindowManager manager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int radius = menuButton.getHeight() / 2;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            closeOverlay = ViewAnimationUtils.createCircularReveal(backgroundView,
                    menuButton.getLeft() + radius, menuButton.getTop() + radius,
                    Math.max(size.x, size.y), radius);
        } else {
            closeOverlay = ObjectAnimator.ofFloat(backgroundView, "alpha", 1f, 0f);
        }
        closeOverlay.setDuration(overlayDuration);
        closeOverlay.setInterpolator(new AccelerateDecelerateInterpolator());
        closeOverlay.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                backgroundView.setVisibility(GONE);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            openOverlay = ViewAnimationUtils.createCircularReveal(backgroundView,
                    menuButton.getLeft() + radius, menuButton.getTop() + radius, radius,
                    Math.max(size.x, size.y));
        } else {
            openOverlay = ObjectAnimator.ofFloat(backgroundView, "alpha", 0f, 1f);
        }
        openOverlay.setDuration(overlayDuration);
        openOverlay.setInterpolator(new AccelerateDecelerateInterpolator());
        openOverlay.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                backgroundView.setVisibility(VISIBLE);
            }
        });
    }

    protected void startCloseAnimator() {
        closeSet.start();
        closeOverlay.start();
        for (ChildAnimator anim : itemAnimators) {
            anim.startCloseAnimator();
        }
    }

    protected void startOpenAnimator() {
        openOverlay.start();
        openSet.start();
        for (ChildAnimator anim : itemAnimators) {
            anim.startOpenAnimator();
        }
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

    private void createDefaultIconAnimation() {
        Animator.AnimatorListener listener = new Animator.AnimatorListener() {
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

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        };

        ObjectAnimator collapseAnimator = ObjectAnimator.ofFloat(menuButton, "rotation", 135f, 0f);
        ObjectAnimator expandAnimator = ObjectAnimator.ofFloat(menuButton, "rotation", 0f, 135f);

        openSet.playTogether(expandAnimator);
        closeSet.playTogether(collapseAnimator);

        openSet.setInterpolator(DEFAULT_OPEN_INTERPOLATOR);
        closeSet.setInterpolator(DEFAULT_CLOSE_INTERPOLATOR);

        openSet.setDuration(actionsDuration);
        closeSet.setDuration(actionsDuration);

        openSet.addListener(listener);
        closeSet.addListener(listener);
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
            // We're dependent on all SnackbarLayouts (if enabled)
            return dependency instanceof Snackbar.SnackbarLayout;
        }

        @Override
        public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionMenu child, View dependency) {
            float translationY = Math.min(0, dependency.getTranslationY() - dependency.getHeight());
            child.setTranslationY(translationY);
            return true;
        }
    }

    class ChildAnimator implements Animator.AnimatorListener {
        private View view;
        private View label;
        private AlphaAnimation openAnimation;
        private AlphaAnimation closeAnimation;
        private boolean playingOpenAnimator;

        ChildAnimator(View view) {
            view.animate().setListener(this);
            if (displayLabels) {
                label = (View) view.getTag();
                openAnimation = new AlphaAnimation(0f, 1f);
                openAnimation.setDuration(250);

                closeAnimation = new AlphaAnimation(1f, 0f);
                closeAnimation.setDuration(250);
            }
            this.view = view;
        }

        void startOpenAnimator() {
            view.animate().cancel();
            playingOpenAnimator = true;

            view.animate()
                    .translationY(0)
                    .setInterpolator(DEFAULT_OPEN_INTERPOLATOR)
                    .start();
        }

        void startCloseAnimator() {
            view.animate().cancel();
            playingOpenAnimator = false;

            if (displayLabels) {
                label.startAnimation(closeAnimation);
            }
            view.animate()
                    .translationY((menuButton.getTop() - view.getTop()))
                    .setInterpolator(DEFAULT_CLOSE_INTERPOLATOR)
                    .start();
        }

        @Override
        public void onAnimationStart(Animator animation) {
            if (playingOpenAnimator) {
                view.setVisibility(VISIBLE);
            } else {
                if (displayLabels) {
                    label.setVisibility(GONE);
                }
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (!playingOpenAnimator) {
                view.setVisibility(GONE);
            } else {
                if (displayLabels) {
                    label.setVisibility(VISIBLE);
                    label.startAnimation(openAnimation);
                }
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }
}
