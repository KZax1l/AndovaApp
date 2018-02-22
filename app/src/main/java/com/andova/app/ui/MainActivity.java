package com.andova.app.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.andova.app.R;

/**
 * Created by Administrator on 2018-02-22.
 *
 * @author kzaxil
 * @since 1.0.0
 */
public class MainActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_main);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rv_module);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.addItemDecoration(new Decoration());
        recyclerView.setAdapter(new Adapter());
    }

    private static class Adapter extends RecyclerView.Adapter<ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(new ImageView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (holder.ivIcon == null) return;
            holder.ivIcon.setImageResource(R.mipmap.ic_module_music);
        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;

        ViewHolder(View itemView) {
            super(itemView);
            if (itemView instanceof ImageView) ivIcon = (ImageView) itemView;
        }
    }

    private static class Decoration extends RecyclerView.ItemDecoration {
        private Paint mPaint;

        Decoration() {
            mPaint = new Paint();
        }

        /**
         * 复写onDraw方法，从而达到在每隔条目的被绘制之前（或之后），让他先帮我们画上去几根线吧
         */
        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            mPaint.setColor(ActivityCompat.getColor(parent.getContext(), R.color.colorToolbar));
            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childView = parent.getChildAt(i);

                float x = childView.getX();
                float y = childView.getY();
                int width = childView.getWidth();
                int height = childView.getHeight();

                if (!isFirstItem(parent, i)) {
                    c.drawLine(x, y + height / 4, x, y + height / 4 * 3, mPaint);
                }
                if (!isLastRaw(parent, i, getSpanCount(parent), childCount)) {
                    c.drawLine(x, y + height, x + width, y + height, mPaint);
                }
            }
            super.onDraw(c, parent, state);
        }

        /**
         * 判断是否是每行第一个项
         */
        private boolean isFirstItem(RecyclerView parent, int position) {
            if (parent.getLayoutManager() instanceof GridLayoutManager) {
                GridLayoutManager layoutManager = (GridLayoutManager) parent.getLayoutManager();
                int spanCount = layoutManager.getSpanCount();
                return position % spanCount == 0;
            }
            return true;
        }

        private int getSpanCount(RecyclerView parent) {
            int spanCount = -1;
            RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
            if (layoutManager instanceof GridLayoutManager) {
                spanCount = ((GridLayoutManager) layoutManager).getSpanCount();
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                spanCount = ((StaggeredGridLayoutManager) layoutManager).getSpanCount();
            }
            return spanCount;
        }

        private boolean isLastRaw(RecyclerView parent, int pos, int spanCount, int childCount) {
            RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
            if (layoutManager instanceof GridLayoutManager) {
                childCount = childCount - (childCount % spanCount == 0 ? spanCount : childCount % spanCount);
                if (pos >= childCount) {
                    return true;
                }
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                int orientation = ((StaggeredGridLayoutManager) layoutManager).getOrientation();
                if (orientation == StaggeredGridLayoutManager.VERTICAL) {
                    childCount = childCount - (childCount % spanCount == 0 ? spanCount : childCount % spanCount);
                    if (pos >= childCount)
                        return true;
                } else {
                    if ((pos + 1) % spanCount == 0) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
