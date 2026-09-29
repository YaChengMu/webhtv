package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.bean.TmdbVideo;
import com.fongmi.android.tv.databinding.AdapterTmdbVideoBinding;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

public class TmdbVideoPresenter extends Presenter {

    // 统一焦点规范（详见 resources/drawable/selector_video_item.xml）：
    // 获得焦点 3dp @color/tv_item_focus_ring，常态 1dp @color/tv_item_normal_stroke。
    private static final int STROKE_FOCUSED = 0xFFFFD166;
    private static final int STROKE_NORMAL = 0x33FFFFFF;
    private static final int STROKE_WIDTH_FOCUSED_DP = 3;
    private static final int STROKE_WIDTH_NORMAL_DP = 1;

    public interface OnClickListener {
        void onItemClick(TmdbVideo item);
    }

    private final OnClickListener listener;

    public TmdbVideoPresenter(OnClickListener listener) {
        this.listener = listener;
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterTmdbVideoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        TmdbVideo video = (TmdbVideo) item;
        ViewHolder holder = (ViewHolder) viewHolder;
        String name = video.getName().isEmpty() ? video.getDisplayType() : video.getName();
        holder.binding.title.setText(name);
        holder.binding.subtitle.setText(video.getDisplayType() + " ? " + video.getScopeLabel());
        ImgUtil.load(name, video.getThumbnailUrl(), holder.binding.poster, true, 300, 169);
        setOnClickListener(holder, view -> {
            if (listener != null) listener.onItemClick(video);
        });
        holder.view.setOnFocusChangeListener((view, focused) -> {
            float scale = focused ? 1.04f : 1.0f;
            view.animate().scaleX(scale).scaleY(scale).setDuration(120).start();
            holder.binding.getRoot().setStrokeWidth(ResUtil.dp2px(focused ? STROKE_WIDTH_FOCUSED_DP : STROKE_WIDTH_NORMAL_DP));
            holder.binding.getRoot().setStrokeColor(focused ? STROKE_FOCUSED : STROKE_NORMAL);
        });
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        viewHolder.view.setOnFocusChangeListener(null);
    }

    static final class ViewHolder extends Presenter.ViewHolder {
        private final AdapterTmdbVideoBinding binding;

        ViewHolder(@NonNull AdapterTmdbVideoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
