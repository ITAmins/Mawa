package com.example;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {
    private final OnExpenseActionListener actionListener;
    private final List<ExpenseModel> expenseList;

    public interface OnExpenseActionListener {
        void onEditClick(ExpenseModel expense, int position);
        void onDeleteClick(ExpenseModel expense, int position);
    }

    public ExpenseAdapter(List<ExpenseModel> expenseList, OnExpenseActionListener actionListener) {
        this.expenseList = expenseList;
        this.actionListener = actionListener;
    }

    @Override
    public ExpenseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ExpenseViewHolder holder, int position) {
        ExpenseModel expense = this.expenseList.get(position);
        holder.bind(expense, position, this.actionListener);
    }

    @Override
    public int getItemCount() {
        return this.expenseList.size();
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private final ImageButton btnDeleteItem;
        private final ImageButton btnEditItem;
        private final FrameLayout layoutCategoryAvatar;
        private final TextView tvCategoryLetter;
        private final TextView tvItemAmount;
        private final TextView tvItemDate;
        private final TextView tvItemName;
        private final TextView tvItemTime;

        public ExpenseViewHolder(View itemView) {
            super(itemView);
            this.tvItemName = (TextView) itemView.findViewById(R.id.tvItemName);
            this.tvItemDate = (TextView) itemView.findViewById(R.id.tvItemDate);
            this.tvItemTime = (TextView) itemView.findViewById(R.id.tvItemTime);
            this.tvItemAmount = (TextView) itemView.findViewById(R.id.tvItemAmount);
            this.btnEditItem = (ImageButton) itemView.findViewById(R.id.btnEditItem);
            this.btnDeleteItem = (ImageButton) itemView.findViewById(R.id.btnDeleteItem);
            this.layoutCategoryAvatar = (FrameLayout) itemView.findViewById(R.id.layoutCategoryAvatar);
            this.tvCategoryLetter = (TextView) itemView.findViewById(R.id.tvCategoryLetter);
        }

        public void bind(final ExpenseModel expense, final int position, final OnExpenseActionListener listener) {
            int serialNo = position + 1;
            String banglaSerial = PdfExporter.toBengaliDigits(String.valueOf(serialNo));
            this.tvCategoryLetter.setText(banglaSerial);
            this.tvItemName.setText(expense.getName());
            this.tvItemDate.setText(expense.getDate());
            this.tvItemTime.setText(expense.getTime());
            String banglaPriceStr = "৳ " + PdfExporter.formatBengaliNumber(expense.getAmount());
            this.tvItemAmount.setText(banglaPriceStr);
            
            String[] badgeColors = {
                "#2563EB", "#059669", "#7C3AED", "#D97706", "#DC2626", 
                "#0891B2", "#4F46E5", "#0D9488", "#EA580C", "#9333EA"
            };
            String colorHex = badgeColors[position % badgeColors.length];
            try {
                this.layoutCategoryAvatar.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorHex)));
            } catch (Exception ignored) {
            }

            this.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onEditClick(expense, position);
                    }
                }
            });

            if (this.btnEditItem != null) {
                this.btnEditItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (listener != null) {
                            listener.onEditClick(expense, position);
                        }
                    }
                });
            }

            if (this.btnDeleteItem != null) {
                this.btnDeleteItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (listener != null) {
                            listener.onDeleteClick(expense, position);
                        }
                    }
                });
            }
        }
    }
}
