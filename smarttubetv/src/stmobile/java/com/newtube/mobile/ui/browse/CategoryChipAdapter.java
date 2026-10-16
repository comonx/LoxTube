package com.newtube.mobile.ui.browse;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

public class CategoryChipAdapter extends RecyclerView.Adapter<CategoryChipAdapter.ChipViewHolder> {
    public interface OnCategorySelectedListener {
        void onCategorySelected(CategoryChip chip);
        void onCategoryReselected(CategoryChip chip);
    }

    private final List<CategoryChip> mItems = new ArrayList<>();
    private final OnCategorySelectedListener mListener;
    private int mSelectedPosition = 0;

    public CategoryChipAdapter(OnCategorySelectedListener listener) {
        mListener = listener;
    }

    public void setItems(List<CategoryChip> items) {
        mItems.clear();
        if (items != null) {
            mItems.addAll(items);
        }
        notifyDataSetChanged();
    }

    public CategoryChip getSelectedChip() {
        if (mSelectedPosition >= 0 && mSelectedPosition < mItems.size()) {
            return mItems.get(mSelectedPosition);
        }
        return !mItems.isEmpty() ? mItems.get(0) : null;
    }

    public void selectChipById(String chipId) {
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i).getId().equals(chipId)) {
                if (mSelectedPosition != i) {
                    int prev = mSelectedPosition;
                    mSelectedPosition = i;
                    notifyItemChanged(prev);
                    notifyItemChanged(mSelectedPosition);
                }
                return;
            }
        }
    }

    public void resetToAll() {
        if (mSelectedPosition != 0) {
            int prev = mSelectedPosition;
            mSelectedPosition = 0;
            notifyItemChanged(prev);
            notifyItemChanged(mSelectedPosition);
        }
    }

    @NonNull
    @Override
    public ChipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mobile_category_chip, parent, false);
        return new ChipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChipViewHolder holder, int position) {
        CategoryChip item = mItems.get(position);
        holder.bind(item, position == mSelectedPosition);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    class ChipViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTextView;

        ChipViewHolder(@NonNull View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.category_chip_text);
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION || mListener == null) {
                    return;
                }
                CategoryChip chip = mItems.get(pos);
                if (chip.isAction()) {
                    mListener.onCategorySelected(chip);
                    return;
                }
                if (pos != mSelectedPosition) {
                    int prev = mSelectedPosition;
                    mSelectedPosition = pos;
                    notifyItemChanged(prev);
                    notifyItemChanged(mSelectedPosition);
                    mListener.onCategorySelected(chip);
                } else {
                    mListener.onCategoryReselected(chip);
                }
            });
        }

        void bind(CategoryChip item, boolean isSelected) {
            mTextView.setText(item.getTitle());
            mTextView.setSelected(!item.isAction() && isSelected);
        }
    }
}
