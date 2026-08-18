package com.example;

import android.accounts.AccountManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.core.view.PointerIconCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.ExpenseAdapter;
import com.example.GoogleSheetsSyncManager;
import com.example.MainActivity;
import com.example.MainViewModel;
import com.example.databinding.ActivityMainBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/* loaded from: classes5.dex */
public class MainActivity extends AppCompatActivity {
    private ExpenseAdapter adapter;
    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private List<ExpenseModel> allExpenses = new ArrayList();
    private String searchFilterText = "";
    private boolean isUpdatingInputs = false;
    private boolean isExpensesExpanded = false;
    private boolean isDashboardFilterThisMonth = false;
    private static final int COLLAPSED_EXPENSES_LIMIT = 5;
    private final Handler backupHandler = new Handler(Looper.getMainLooper());
    private final Runnable backupRunnable = new Runnable() { // from class: com.example.MainActivity$$ExternalSyntheticLambda59
        @Override // java.lang.Runnable
        public final void run() {
            MainActivity.this.triggerAutoCloudBackup();
        }
    };

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_MyApplication);
        super.onCreate(savedInstanceState);
        try {
            this.binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(this.binding.getRoot());
            if (this.binding.toolbar != null) {
                setSupportActionBar(this.binding.toolbar);
            }
            this.viewModel = (MainViewModel) new ViewModelProvider(this).get(MainViewModel.class);
            if (this.binding.rvExpenses != null) {
                this.binding.rvExpenses.setLayoutManager(new LinearLayoutManager(this));
            }
            observeViewModel();
            setupListeners();
            setupDashboard();
            setupCloudBackup();
            setupLocalBackup();
            setupBakiKhata();
            setupFordiKhata();
            setupAutocomplete();
        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "Fatal error in onCreate: " + t.getMessage(), t);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void filterExpenses() {
        if (this.binding == null) {
            return;
        }
        List<ExpenseModel> filteredList = new ArrayList<>();
        boolean isSearching = this.searchFilterText != null && !this.searchFilterText.trim().isEmpty();
        if (!isSearching) {
            filteredList.addAll(this.allExpenses);
        } else {
            String query = this.searchFilterText.toLowerCase().trim();
            for (ExpenseModel exp : this.allExpenses) {
                String expName = exp.getName() != null ? exp.getName().toLowerCase() : "";
                String expAmount = String.valueOf(exp.getAmount());
                String expTime = exp.getTime() != null ? exp.getTime().toLowerCase() : "";
                if (expName.contains(query) || expAmount.contains(query) || expTime.contains(query)) {
                    filteredList.add(exp);
                }
            }
        }
        
        int totalCount = filteredList.size();
        if (this.binding.tvExpensesCountBadge != null) {
            this.binding.tvExpensesCountBadge.setText(PdfExporter.toBengaliDigits(String.valueOf(totalCount)) + "টি");
        }

        boolean isEmpty = filteredList.isEmpty();
        if (isEmpty) {
            this.binding.layoutEmptyState.setVisibility(View.VISIBLE);
            this.binding.rvExpenses.setVisibility(View.GONE);
            if (this.binding.btnToggleExpensesCollapse != null) {
                this.binding.btnToggleExpensesCollapse.setVisibility(View.GONE);
            }
        } else {
            this.binding.layoutEmptyState.setVisibility(View.GONE);
            this.binding.rvExpenses.setVisibility(View.VISIBLE);

            List<ExpenseModel> displayList;
            if (!isSearching && totalCount > COLLAPSED_EXPENSES_LIMIT && !isExpensesExpanded) {
                displayList = new ArrayList<>(filteredList.subList(0, COLLAPSED_EXPENSES_LIMIT));
                if (this.binding.btnToggleExpensesCollapse != null) {
                    this.binding.btnToggleExpensesCollapse.setVisibility(View.VISIBLE);
                    this.binding.btnToggleExpensesCollapse.setText("সবগুলো দেখুন (" + PdfExporter.toBengaliDigits(String.valueOf(totalCount)) + "টি খরচ) ▾");
                }
            } else if (!isSearching && totalCount > COLLAPSED_EXPENSES_LIMIT && isExpensesExpanded) {
                displayList = filteredList;
                if (this.binding.btnToggleExpensesCollapse != null) {
                    this.binding.btnToggleExpensesCollapse.setVisibility(View.VISIBLE);
                    this.binding.btnToggleExpensesCollapse.setText("কম দেখুন (সংক্ষেপ করুন) ▴");
                }
            } else {
                displayList = filteredList;
                if (this.binding.btnToggleExpensesCollapse != null) {
                    this.binding.btnToggleExpensesCollapse.setVisibility(View.GONE);
                }
            }

            this.adapter = new ExpenseAdapter(displayList, new ExpenseAdapter.OnExpenseActionListener() {
                @Override
                public void onEditClick(ExpenseModel expense, int position) {
                    MainActivity.this.showEditExpenseDialog(expense);
                }

                @Override
                public void onDeleteClick(ExpenseModel expense, int position) {
                    MainActivity.this.showDeleteConfirmationDialog(expense);
                }
            });
            this.binding.rvExpenses.setAdapter(this.adapter);
        }
        updateNotebookTextPreview();
    }

    private void observeViewModel() {
        this.viewModel.getExpenses().observe(this, new Observer() { // from class: com.example.MainActivity$$ExternalSyntheticLambda64
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                MainActivity.this.m6974lambda$observeViewModel$0$comexampleMainActivity((List) obj);
            }
        });
        this.viewModel.getTotalExpenses().observe(this, new Observer() { // from class: com.example.MainActivity$$ExternalSyntheticLambda65
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                MainActivity.this.m6975lambda$observeViewModel$1$comexampleMainActivity((Double) obj);
            }
        });
        this.viewModel.getSabekCash().observe(this, new Observer() { // from class: com.example.MainActivity$$ExternalSyntheticLambda67
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                MainActivity.this.m6976lambda$observeViewModel$2$comexampleMainActivity((Double) obj);
            }
        });
        this.viewModel.getAvailableCash().observe(this, new Observer() { // from class: com.example.MainActivity$$ExternalSyntheticLambda68
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                MainActivity.this.m6977lambda$observeViewModel$3$comexampleMainActivity((Double) obj);
            }
        });
        this.viewModel.getDailySale().observe(this, new Observer() { // from class: com.example.MainActivity$$ExternalSyntheticLambda69
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                MainActivity.this.m6978lambda$observeViewModel$4$comexampleMainActivity((Double) obj);
            }
        });
        this.viewModel.getTotalSale().observe(this, new Observer() { // from class: com.example.MainActivity$$ExternalSyntheticLambda70
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                MainActivity.this.m6979lambda$observeViewModel$5$comexampleMainActivity((Double) obj);
            }
        });
        this.viewModel.getCalculationResult().observe(this, new Observer() { // from class: com.example.MainActivity$$ExternalSyntheticLambda71
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                MainActivity.this.m6980lambda$observeViewModel$6$comexampleMainActivity((Double) obj);
            }
        });
        this.viewModel.getActiveDateString().observe(this, new Observer() { // from class: com.example.MainActivity$$ExternalSyntheticLambda72
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                MainActivity.this.m6981lambda$observeViewModel$7$comexampleMainActivity((String) obj);
            }
        });
        this.viewModel.getActiveDayOfWeek().observe(this, new Observer() { // from class: com.example.MainActivity$$ExternalSyntheticLambda73
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                MainActivity.this.m6982lambda$observeViewModel$8$comexampleMainActivity((String) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$observeViewModel$0$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6974lambda$observeViewModel$0$comexampleMainActivity(List list) {
        this.allExpenses = list != null ? list : new ArrayList();
        filterExpenses();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$observeViewModel$1$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6975lambda$observeViewModel$1$comexampleMainActivity(Double amount) {
        String bFormatted = "৳ " + PdfExporter.formatBengaliNumber(amount.doubleValue());
        this.binding.tvTotalExpenses.setText(bFormatted);
        updateNotebookTextPreview();
        updateHeroCard();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$observeViewModel$2$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6976lambda$observeViewModel$2$comexampleMainActivity(Double sabek) {
        if (!this.isUpdatingInputs) {
            this.isUpdatingInputs = true;
            if (sabek.doubleValue() == 0.0d) {
                if (this.binding.etSabekCash.getText().length() > 0) {
                    try {
                        double currVal = Double.parseDouble(this.binding.etSabekCash.getText().toString());
                        if (currVal != 0.0d) {
                            this.binding.etSabekCash.setText("");
                        }
                    } catch (Exception e) {
                    }
                }
            } else {
                String strVal = String.valueOf(sabek);
                if (strVal.endsWith(".0")) {
                    strVal = strVal.substring(0, strVal.length() - 2);
                }
                if (!this.binding.etSabekCash.getText().toString().equals(strVal)) {
                    this.binding.etSabekCash.setText(strVal);
                }
            }
            this.isUpdatingInputs = false;
        }
        updateSabekSuggestionUI();
        updateNotebookTextPreview();
        updateHeroCard();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$observeViewModel$3$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6977lambda$observeViewModel$3$comexampleMainActivity(Double cash) {
        if (!this.isUpdatingInputs) {
            this.isUpdatingInputs = true;
            if (cash.doubleValue() == 0.0d) {
                if (this.binding.etAvailableCash.getText().length() > 0) {
                    try {
                        double currVal = Double.parseDouble(this.binding.etAvailableCash.getText().toString());
                        if (currVal != 0.0d) {
                            this.binding.etAvailableCash.setText("");
                        }
                    } catch (Exception e) {
                    }
                }
            } else {
                String strVal = String.valueOf(cash);
                if (strVal.endsWith(".0")) {
                    strVal = strVal.substring(0, strVal.length() - 2);
                }
                if (!this.binding.etAvailableCash.getText().toString().equals(strVal)) {
                    this.binding.etAvailableCash.setText(strVal);
                }
            }
            this.isUpdatingInputs = false;
        }
        this.binding.tvMiniAvailableCash.setText("৳ " + PdfExporter.formatBengaliNumber(cash.doubleValue()));
        updateSabekSuggestionUI();
        updateNotebookTextPreview();
        updateHeroCard();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$observeViewModel$4$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6978lambda$observeViewModel$4$comexampleMainActivity(Double sale) {
        String bFormatted = "৳ " + PdfExporter.formatBengaliNumber(sale.doubleValue());
        this.binding.tvDailySaleAuto.setText(bFormatted);
        this.binding.tvMiniDailySale.setText(bFormatted);
        updateNotebookTextPreview();
        updateHeroCard();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$observeViewModel$5$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6979lambda$observeViewModel$5$comexampleMainActivity(Double totalSale) {
        String bFormatted = "৳ " + PdfExporter.formatBengaliNumber(totalSale.doubleValue());
        this.binding.tvTotalSale.setText(bFormatted);
        this.binding.tvMiniTotalSale.setText(bFormatted);
        updateNotebookTextPreview();
        updateHeroCard();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$observeViewModel$6$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6980lambda$observeViewModel$6$comexampleMainActivity(Double result) {
        updateResultCard(result.doubleValue());
        updateNotebookTextPreview();
        updateHeroCard();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$observeViewModel$7$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6981lambda$observeViewModel$7$comexampleMainActivity(String date) {
        this.binding.tvActiveDateDisplay.setText(formatBengaliLongDate(date));
        updateNotebookTextPreview();
        updateHeroCard();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$observeViewModel$8$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6982lambda$observeViewModel$8$comexampleMainActivity(String day) {
        this.binding.tvActiveDayDisplay.setText(", " + day);
        updateNotebookTextPreview();
        updateHeroCard();
    }

    /* JADX WARN: Removed duplicated region for block: B:114:0x027a  */
    /* JADX WARN: Removed duplicated region for block: B:25:0x028a  */
    /* JADX WARN: Removed duplicated region for block: B:28:0x02a3  */
    /* JADX WARN: Removed duplicated region for block: B:31:0x0309  */
    /* JADX WARN: Removed duplicated region for block: B:40:0x0133 A[Catch: Exception -> 0x027d, TRY_LEAVE, TryCatch #1 {Exception -> 0x027d, blocks: (B:38:0x0127, B:40:0x0133, B:49:0x0232, B:51:0x024b), top: B:37:0x0127 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void updateHeroCard() {
        if (this.binding == null || this.viewModel == null) {
            return;
        }
        try {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            String greeting;
            if (hour >= 4 && hour < 12) {
                greeting = "শুভ সকাল, মাওয়া স্টোর! 🌅";
            } else if (hour >= 12 && hour < 15) {
                greeting = "শুভ দুপুর, মাওয়া স্টোর! ☀️";
            } else if (hour >= 15 && hour < 18) {
                greeting = "শুভ বিকাল, মাওয়া স্টোর! 🌇";
            } else if (hour >= 18 && hour < 22) {
                greeting = "শুভ সন্ধ্যা, মাওয়া স্টোর! 🌆";
            } else {
                greeting = "শুভ রাত্রি, মাওয়া স্টোর! 🌙";
            }
            if (this.binding.tvHeroGreeting != null) {
                this.binding.tvHeroGreeting.setText(greeting);
            }

            String activeDate = this.viewModel.getActiveDateString().getValue();
            String activeDayOfWeek = this.viewModel.getActiveDayOfWeek().getValue();
            if (activeDate != null && activeDate.length() >= 10) {
                String[] parts = activeDate.split("-");
                if (parts.length == 3) {
                    String dayStr = parts[0];
                    String monthNum = parts[1];
                    String monthName;
                    switch (monthNum) {
                        case "01": monthName = "জানুয়ারি"; break;
                        case "02": monthName = "ফেব্রুয়ারি"; break;
                        case "03": monthName = "মার্চ"; break;
                        case "04": monthName = "এপ্রিল"; break;
                        case "05": monthName = "মে"; break;
                        case "06": monthName = "জুন"; break;
                        case "07": monthName = "জুলাই"; break;
                        case "08": monthName = "আগস্ট"; break;
                        case "09": monthName = "সেপ্টেম্বর"; break;
                        case "10": monthName = "অক্টোবর"; break;
                        case "11": monthName = "নভেম্বর"; break;
                        case "12": monthName = "ডিসেম্বর"; break;
                        default: monthName = monthNum; break;
                    }
                    if (this.binding.tvHeroDateMonth != null) {
                        this.binding.tvHeroDateMonth.setText(monthName);
                    }
                    if (this.binding.tvHeroDateDay != null) {
                        this.binding.tvHeroDateDay.setText(PdfExporter.toBengaliDigits(dayStr));
                    }
                }
            }
            if (this.binding.tvHeroDateDayOfWeek != null) {
                this.binding.tvHeroDateDayOfWeek.setText(activeDayOfWeek != null ? activeDayOfWeek : "");
            }

            Double dailySale = this.viewModel.getDailySale().getValue();
            double dailySaleVal = dailySale != null ? dailySale.doubleValue() : 0.0d;
            if (this.binding.tvHeroDailySale != null) {
                this.binding.tvHeroDailySale.setText("৳ " + PdfExporter.formatBengaliNumber(dailySaleVal));
            }

            Double totalExpenses = this.viewModel.getTotalExpenses().getValue();
            double totalExpVal = totalExpenses != null ? totalExpenses.doubleValue() : 0.0d;
            if (this.binding.tvHeroDailyExpense != null) {
                this.binding.tvHeroDailyExpense.setText("৳ " + PdfExporter.formatBengaliNumber(totalExpVal));
            }

            Double availableCash = this.viewModel.getAvailableCash().getValue();
            double availCashVal = availableCash != null ? availableCash.doubleValue() : 0.0d;
            if (this.binding.tvHeroAvailableCash != null) {
                this.binding.tvHeroAvailableCash.setText("৳ " + PdfExporter.formatBengaliNumber(availCashVal));
            }

            Double result = this.viewModel.getCalculationResult().getValue();
            double resultVal = result != null ? result.doubleValue() : 0.0d;
            if (this.binding.tvHeroResult != null) {
                this.binding.tvHeroResult.setText("৳ " + PdfExporter.formatBengaliNumber(Math.abs(resultVal)));
            }

            if (resultVal > 0.0d) {
                if (this.binding.tvHeroStatusBadge != null) {
                    this.binding.tvHeroStatusBadge.setText("লাভ");
                    this.binding.tvHeroStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#34D399")));
                    this.binding.tvHeroStatusBadge.setTextColor(Color.parseColor("#064E3B"));
                }
                if (this.binding.tvHeroResult != null) {
                    this.binding.tvHeroResult.setTextColor(Color.parseColor("#34D399"));
                }
                if (this.binding.tvHeroCompareStatus != null) {
                    this.binding.tvHeroCompareStatus.setText("লাভ হয়েছে ৳ " + PdfExporter.formatBengaliNumber(resultVal));
                }
            } else if (resultVal < 0.0d) {
                if (this.binding.tvHeroStatusBadge != null) {
                    this.binding.tvHeroStatusBadge.setText("ঘাটতি");
                    this.binding.tvHeroStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F87171")));
                    this.binding.tvHeroStatusBadge.setTextColor(Color.parseColor("#7F1D1D"));
                }
                if (this.binding.tvHeroResult != null) {
                    this.binding.tvHeroResult.setTextColor(Color.parseColor("#F87171"));
                }
                if (this.binding.tvHeroCompareStatus != null) {
                    this.binding.tvHeroCompareStatus.setText("ঘাটতি হয়েছে ৳ " + PdfExporter.formatBengaliNumber(Math.abs(resultVal)));
                }
            } else {
                if (this.binding.tvHeroStatusBadge != null) {
                    this.binding.tvHeroStatusBadge.setText("সমান");
                    this.binding.tvHeroStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#93C5FD")));
                    this.binding.tvHeroStatusBadge.setTextColor(Color.parseColor("#1E3A8A"));
                }
                if (this.binding.tvHeroResult != null) {
                    this.binding.tvHeroResult.setTextColor(Color.parseColor("#FFFFFF"));
                }
                if (this.binding.tvHeroCompareStatus != null) {
                    this.binding.tvHeroCompareStatus.setText("হিসাব সমান রয়েছে");
                }
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error updating hero card", e);
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private String formatBengaliLongDate(String dateStr) {
        String monthName;
        if (dateStr != null) {
            char c = '\n';
            if (dateStr.length() >= 10) {
                try {
                    String[] parts = dateStr.split("-");
                    if (parts.length != 3) {
                        return dateStr;
                    }
                    String day = parts[0];
                    String monthNum = parts[1];
                    String year = parts[2];
                    switch (monthNum.hashCode()) {
                        case 1537:
                            if (monthNum.equals("01")) {
                                c = 0;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1538:
                            if (monthNum.equals("02")) {
                                c = 1;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1539:
                            if (monthNum.equals("03")) {
                                c = 2;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1540:
                            if (monthNum.equals("04")) {
                                c = 3;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1541:
                            if (monthNum.equals("05")) {
                                c = 4;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1542:
                            if (monthNum.equals("06")) {
                                c = 5;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1543:
                            if (monthNum.equals("07")) {
                                c = 6;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1544:
                            if (monthNum.equals("08")) {
                                c = 7;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1545:
                            if (monthNum.equals("09")) {
                                c = '\b';
                                break;
                            }
                            c = 65535;
                            break;
                        case 1567:
                            if (monthNum.equals("10")) {
                                c = '\t';
                                break;
                            }
                            c = 65535;
                            break;
                        case 1568:
                            if (monthNum.equals("11")) {
                                break;
                            }
                            c = 65535;
                            break;
                        case 1569:
                            if (monthNum.equals("12")) {
                                c = 11;
                                break;
                            }
                            c = 65535;
                            break;
                        default:
                            c = 65535;
                            break;
                    }
                    switch (c) {
                        case 0:
                            monthName = "জানুয়ারি";
                            break;
                        case 1:
                            monthName = "ফেব্রুয়ারি";
                            break;
                        case 2:
                            monthName = "মার্চ";
                            break;
                        case 3:
                            monthName = "এপ্রিল";
                            break;
                        case 4:
                            monthName = "মে";
                            break;
                        case 5:
                            monthName = "জুন";
                            break;
                        case 6:
                            monthName = "জুলাই";
                            break;
                        case 7:
                            monthName = "আগস্ট";
                            break;
                        case '\b':
                            monthName = "সেপ্টেম্বর";
                            break;
                        case '\t':
                            monthName = "অক্টোবর";
                            break;
                        case '\n':
                            monthName = "নভেম্বর";
                            break;
                        case 11:
                            monthName = "ডিসেম্বর";
                            break;
                        default:
                            monthName = monthNum;
                            break;
                    }
                    return day + " " + monthName + " " + year;
                } catch (Exception e) {
                    return dateStr;
                }
            }
        }
        return dateStr;
    }

    private void updateResultCard(double result) {
        if (result > 0.0d) {
            this.binding.cardResult.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#DCFCE7")));
            this.binding.tvResultStatus.setText("🟢 লাভ হয়েছে");
            this.binding.tvResultStatus.setTextColor(Color.parseColor("#16A34A"));
            this.binding.tvResultAmount.setText("৳ " + PdfExporter.formatBengaliNumber(result));
            this.binding.tvResultAmount.setTextColor(Color.parseColor("#16A34A"));
            this.binding.tvResultMessage.setText("আজকের বিক্রি খরচের চেয়ে বেশি");
            this.binding.tvResultMessage.setTextColor(Color.parseColor("#15803D"));
            return;
        }
        ActivityMainBinding activityMainBinding = this.binding;
        if (result == 0.0d) {
            activityMainBinding.cardResult.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#ECFDF5")));
            this.binding.tvResultStatus.setText("✅ হিসাব সমান");
            this.binding.tvResultStatus.setTextColor(Color.parseColor("#059669"));
            this.binding.tvResultAmount.setText("৳ 0");
            this.binding.tvResultAmount.setTextColor(Color.parseColor("#059669"));
            this.binding.tvResultMessage.setText("বিক্রি ও খরচ সমান");
            this.binding.tvResultMessage.setTextColor(Color.parseColor("#047857"));
            return;
        }
        activityMainBinding.cardResult.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
        this.binding.tvResultStatus.setText("🔴 ঘাটতি");
        this.binding.tvResultStatus.setTextColor(Color.parseColor("#DC2626"));
        this.binding.tvResultAmount.setText("৳ " + PdfExporter.formatBengaliNumber(Math.abs(result)));
        this.binding.tvResultAmount.setTextColor(Color.parseColor("#DC2626"));
        this.binding.tvResultMessage.setText("খরচ বিক্রির চেয়ে বেশি হয়েছে");
        this.binding.tvResultMessage.setTextColor(Color.parseColor("#B91C1C"));
    }

    private void updateNotebookTextPreview() {
        if (this.viewModel != null && this.binding != null) {
            String report = this.viewModel.generateRuledNotebookReport();
            this.binding.tvNotebookReportBody.setText(report);
            List<ExpenseModel> currentExpenses = this.viewModel.getExpenses().getValue();
            this.binding.layoutNotebookExpensesList.removeAllViews();
            if (currentExpenses == null || currentExpenses.isEmpty()) {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("কোনো খরচ নেই");
                tvEmpty.setTextColor(Color.parseColor("#94A3B8"));
                tvEmpty.setTextSize(11.0f);
                tvEmpty.setGravity(1);
                this.binding.layoutNotebookExpensesList.addView(tvEmpty);
            } else {
                for (ExpenseModel expense : currentExpenses) {
                    LinearLayout row = new LinearLayout(this);
                    row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
                    row.setOrientation(0);
                    row.setPadding(0, 4, 0, 4);
                    TextView tvName = new TextView(this);
                    tvName.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                    tvName.setText(expense.getName());
                    tvName.setTextColor(Color.parseColor("#334155"));
                    tvName.setTextSize(11.0f);
                    tvName.setMaxLines(1);
                    tvName.setEllipsize(TextUtils.TruncateAt.END);
                    TextView tvDots = new TextView(this);
                    tvDots.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
                    tvDots.setText("......");
                    tvDots.setTextColor(Color.parseColor("#CBD5E1"));
                    tvDots.setTextSize(10.0f);
                    TextView tvVal = new TextView(this);
                    tvVal.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
                    tvVal.setText("৳" + PdfExporter.formatBengaliNumber(expense.getAmount()));
                    tvVal.setTextColor(Color.parseColor("#0F172A"));
                    tvVal.setTextSize(11.0f);
                    tvVal.setPadding(4, 0, 0, 0);
                    row.addView(tvName);
                    row.addView(tvDots);
                    row.addView(tvVal);
                    this.binding.layoutNotebookExpensesList.addView(row);
                }
            }
            Double totalExpVal = this.viewModel.getTotalExpenses().getValue();
            double totalExpenses = totalExpVal != null ? totalExpVal.doubleValue() : 0.0d;
            this.binding.tvNotebookTotalExpenses.setText("মোট খরচ: ৳" + PdfExporter.formatBengaliNumber(totalExpenses));
            Double dailySaleVal = this.viewModel.getDailySale().getValue();
            double dailySale = dailySaleVal != null ? dailySaleVal.doubleValue() : 0.0d;
            this.binding.tvNotebookDailySale.setText("আজকের বেচা.. ৳" + PdfExporter.formatBengaliNumber(dailySale));
            Double avCashVal = this.viewModel.getAvailableCash().getValue();
            double avCash = avCashVal != null ? avCashVal.doubleValue() : 0.0d;
            this.binding.tvNotebookAvailableCash.setText("আছে.......... ৳" + PdfExporter.formatBengaliNumber(avCash));
            Double sabekVal = this.viewModel.getSabekCash().getValue();
            double sabekCash = sabekVal != null ? sabekVal.doubleValue() : 0.0d;
            this.binding.tvNotebookSabekCash.setText("সাবেক....... ৳" + PdfExporter.formatBengaliNumber(sabekCash));
            Double totSaleVal = this.viewModel.getTotalSale().getValue();
            double totalSale = totSaleVal != null ? totSaleVal.doubleValue() : 0.0d;
            this.binding.tvNotebookTotalSale.setText("মোট বেচা ৳" + PdfExporter.formatBengaliNumber(totalSale));
            Double resultVal = this.viewModel.getCalculationResult().getValue();
            double result = resultVal != null ? resultVal.doubleValue() : 0.0d;
            if (result > 0.0d) {
                this.binding.cardNotebookResult.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#DCFCE7")));
                this.binding.tvNotebookResultLabel.setText("লাভ / বাড়তি");
                this.binding.tvNotebookResultLabel.setTextColor(Color.parseColor("#16A34A"));
                this.binding.tvNotebookResultAmount.setText("৳" + PdfExporter.formatBengaliNumber(result));
                this.binding.tvNotebookResultAmount.setTextColor(Color.parseColor("#15803D"));
                this.binding.ivNotebookResultTrend.setImageResource(R.drawable.ic_trend_up);
                this.binding.ivNotebookResultTrend.setImageTintList(ColorStateList.valueOf(Color.parseColor("#16A34A")));
                return;
            }
            ActivityMainBinding activityMainBinding = this.binding;
            if (result == 0.0d) {
                activityMainBinding.cardNotebookResult.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
                this.binding.tvNotebookResultLabel.setText("সমান হিসাব");
                this.binding.tvNotebookResultLabel.setTextColor(Color.parseColor("#475569"));
                this.binding.tvNotebookResultAmount.setText("৳০");
                this.binding.tvNotebookResultAmount.setTextColor(Color.parseColor("#475569"));
                this.binding.ivNotebookResultTrend.setImageDrawable(null);
                return;
            }
            activityMainBinding.cardNotebookResult.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
            this.binding.tvNotebookResultLabel.setText("ঘাটতি");
            this.binding.tvNotebookResultLabel.setTextColor(Color.parseColor("#DC2626"));
            this.binding.tvNotebookResultAmount.setText("৳" + PdfExporter.formatBengaliNumber(Math.abs(result)));
            this.binding.tvNotebookResultAmount.setTextColor(Color.parseColor("#BE185D"));
            this.binding.ivNotebookResultTrend.setImageResource(R.drawable.ic_trend_down);
            this.binding.ivNotebookResultTrend.setImageTintList(ColorStateList.valueOf(Color.parseColor("#DC2626")));
        }
    }

    private void setupListeners() {
        this.binding.etExpenseName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Object item = adapterView.getItemAtPosition(position);
                if (item != null) {
                    binding.etExpenseName.setText(item.toString());
                    binding.etExpenseName.setSelection(item.toString().length());
                }
                binding.etExpenseAmount.requestFocus();
            }
        });
        this.binding.etExpenseName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    binding.etExpenseAmount.requestFocus();
                    return true;
                }
                return false;
            }
        });
        this.binding.etExpenseAmount.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    binding.btnAddExpense.performClick();
                    return true;
                }
                return false;
            }
        });
        View.OnClickListener sabekSuggestClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applySuggestedSabekCash();
            }
        };
        this.binding.btnSuggestSabekCash.setOnClickListener(sabekSuggestClick);
        this.binding.btnApplySabekSuggestion.setOnClickListener(sabekSuggestClick);
        this.binding.btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(MainActivity.this, view);
                popup.getMenu().add(0, 1, 0, "📄 রিপোর্ট শেয়ার করুন");
                popup.getMenu().add(0, 2, 1, "📥 পিডিএফ ডাউনলোড");
                popup.getMenu().add(0, 3, 2, "🔄 হিসাব রিলোড করুন");
                popup.getMenu().add(0, 4, 3, "🗑️ হিসাব রিসেট করুন");
                popup.setOnMenuItemClickListener(new androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(android.view.MenuItem item) {
                        switch (item.getItemId()) {
                            case 1:
                                shareDailyReport();
                                return true;
                            case 2:
                                triggerPdfExport(true);
                                return true;
                            case 3:
                                viewModel.loadSavedData();
                                updateSabekSuggestionUI();
                                updateDashboardUI();
                                Toast.makeText(MainActivity.this, "হিসাব হালনাগাদ করা হয়েছে", Toast.LENGTH_SHORT).show();
                                return true;
                            case 4:
                                showClearAllConfirmationDialog();
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                popup.show();
            }
        });
        this.binding.btnToggleExpensesCollapse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isExpensesExpanded = !isExpensesExpanded;
                filterExpenses();
            }
        });
        this.binding.etSearchExpenses.addTextChangedListener(new TextWatcher() { // from class: com.example.MainActivity.2
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                MainActivity.this.searchFilterText = s != null ? s.toString() : "";
                MainActivity.this.filterExpenses();
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
            }
        });
        this.binding.btnPrevDay.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda30
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m7021lambda$setupListeners$18$comexampleMainActivity(view);
            }
        });
        this.binding.btnNextDay.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda31
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m7022lambda$setupListeners$19$comexampleMainActivity(view);
            }
        });
        this.binding.layoutDatePicker.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda16
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m7023lambda$setupListeners$20$comexampleMainActivity(view);
            }
        });
        this.binding.btnAddExpense.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda17
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m7024lambda$setupListeners$21$comexampleMainActivity(view);
            }
        });
        this.binding.etSabekCash.addTextChangedListener(new TextWatcher() { // from class: com.example.MainActivity.3
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (MainActivity.this.isUpdatingInputs) {
                    return;
                }
                String input = s.toString().trim();
                double val = 0.0d;
                if (!input.isEmpty()) {
                    try {
                        val = Double.parseDouble(input);
                    } catch (NumberFormatException e) {
                    }
                }
                MainActivity.this.viewModel.setSabekCash(val);
                MainActivity.this.planAutoCloudBackup();
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
            }
        });
        this.binding.etAvailableCash.addTextChangedListener(new TextWatcher() { // from class: com.example.MainActivity.4
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (MainActivity.this.isUpdatingInputs) {
                    return;
                }
                String input = s.toString().trim();
                double val = 0.0d;
                if (!input.isEmpty()) {
                    try {
                        val = Double.parseDouble(input);
                    } catch (NumberFormatException e) {
                    }
                }
                MainActivity.this.viewModel.setAvailableCash(val);
                MainActivity.this.planAutoCloudBackup();
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
            }
        });
        this.binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda18
            @Override // androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
            public final void onRefresh() {
                MainActivity.this.m7026lambda$setupListeners$23$comexampleMainActivity();
            }
        });
        this.binding.btnExportPdf.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda19
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m7027lambda$setupListeners$24$comexampleMainActivity(view);
            }
        });
        this.binding.btnShareReport.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda20
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m7028lambda$setupListeners$25$comexampleMainActivity(view);
            }
        });
        this.binding.btnRefreshExpenses.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda21
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m7030lambda$setupListeners$27$comexampleMainActivity(view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupListeners$18$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7021lambda$setupListeners$18$comexampleMainActivity(View v) {
        this.viewModel.moveToPreviousDay();
        updateSabekSuggestionUI();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupListeners$19$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7022lambda$setupListeners$19$comexampleMainActivity(View v) {
        this.viewModel.moveToNextDay();
        updateSabekSuggestionUI();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupListeners$20$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7023lambda$setupListeners$20$comexampleMainActivity(View v) {
        showDatePickerDialog();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupListeners$21$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7024lambda$setupListeners$21$comexampleMainActivity(View v) {
        String name = this.binding.etExpenseName.getText().toString().trim();
        String amountStr = this.binding.etExpenseAmount.getText().toString().trim();
        if (name.isEmpty()) {
            this.binding.etExpenseName.setError("খরচের নাম খালি রাখা যাবে না!");
            Toast.makeText(this, "অনুগ্রহ করে খরচের বিবরণ বা নাম লিখুন", 0).show();
            return;
        }
        if (amountStr.isEmpty()) {
            this.binding.etExpenseAmount.setError("টাকার পরিমাণ লিখুন!");
            Toast.makeText(this, "অনুগ্রহ করে টাকার সঠিক পরিমাণ দিন", 0).show();
            return;
        }
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0.0d) {
                this.binding.etExpenseAmount.setError("টাকার পরিমাণ শূন্য বা ঋণাত্মক হতে পারবে না!");
                Toast.makeText(this, "টাকার পরিমাণ অবশ্যই শূন্যের চেয়ে বড় হতে হবে", 0).show();
                return;
            }
            boolean success = this.viewModel.addExpense(name, amount);
            if (success) {
                this.binding.etExpenseName.setText("");
                this.binding.etExpenseAmount.setText("");
                this.binding.etExpenseName.clearFocus();
                this.binding.etExpenseAmount.clearFocus();
                Toast.makeText(this, "নতুন খরচ খತಿয়ানে যোগ করা হয়েছে", 0).show();
                planAutoCloudBackup();
            }
        } catch (NumberFormatException e) {
            this.binding.etExpenseAmount.setError("সঠিক সংখ্যা দিন!");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupListeners$23$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7026lambda$setupListeners$23$comexampleMainActivity() {
        this.viewModel.loadSavedData();
        updateSabekSuggestionUI();
        updateDashboardUI();
        this.binding.swipeRefreshLayout.postDelayed(new Runnable() { // from class: com.example.MainActivity$$ExternalSyntheticLambda34
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m7025lambda$setupListeners$22$comexampleMainActivity();
            }
        }, 600L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupListeners$22$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7025lambda$setupListeners$22$comexampleMainActivity() {
        this.binding.swipeRefreshLayout.setRefreshing(false);
        Toast.makeText(this, "হিসাব হালনাগাদ (রিলোড) হয়েছে", 0).show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupListeners$24$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7027lambda$setupListeners$24$comexampleMainActivity(View v) {
        triggerPdfExport(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupListeners$25$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7028lambda$setupListeners$25$comexampleMainActivity(View v) {
        shareDailyReport();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupListeners$27$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7030lambda$setupListeners$27$comexampleMainActivity(View v) {
        this.binding.swipeRefreshLayout.setRefreshing(true);
        this.viewModel.loadSavedData();
        updateDashboardUI();
        this.binding.swipeRefreshLayout.postDelayed(new Runnable() { // from class: com.example.MainActivity$$ExternalSyntheticLambda76
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m7029lambda$setupListeners$26$comexampleMainActivity();
            }
        }, 600L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupListeners$26$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7029lambda$setupListeners$26$comexampleMainActivity() {
        this.binding.swipeRefreshLayout.setRefreshing(false);
        Toast.makeText(this, "হিসাব হালনাগাদ (রিলোড) হয়েছে", 0).show();
    }

    private File triggerPdfExport(boolean showToast) {
        double result = this.viewModel.getCalculationResult().getValue() != null ? this.viewModel.getCalculationResult().getValue().doubleValue() : 0.0d;
        double totExp = this.viewModel.getTotalExpenses().getValue() != null ? this.viewModel.getTotalExpenses().getValue().doubleValue() : 0.0d;
        double sale = this.viewModel.getDailySale().getValue() != null ? this.viewModel.getDailySale().getValue().doubleValue() : 0.0d;
        double cash = this.viewModel.getAvailableCash().getValue() != null ? this.viewModel.getAvailableCash().getValue().doubleValue() : 0.0d;
        double totSl = this.viewModel.getTotalSale().getValue() != null ? this.viewModel.getTotalSale().getValue().doubleValue() : 0.0d;
        double sabek = this.viewModel.getSabekCash().getValue() != null ? this.viewModel.getSabekCash().getValue().doubleValue() : 0.0d;
        File file = PdfExporter.exportToPdf(this, this.viewModel.getExpenses().getValue(), totExp, sale, cash, totSl, sabek, result, this.viewModel.getCurrentFormattedDate(), this.viewModel.getBengaliDayOfWeek());
        if (file != null && showToast) {
            Toast.makeText(this, "PDF রিপোর্ট তৈরি হয়েছে!\nফাইল: " + file.getName(), 1).show();
        }
        return file;
    }

    private void shareDailyReport() {
        File pdfFile = triggerPdfExport(false);
        String textSummary = this.viewModel.generateRuledNotebookReport();
        Intent shareIntent = new Intent("android.intent.action.SEND");
        shareIntent.setType("application/pdf");
        shareIntent.putExtra("android.intent.extra.SUBJECT", "দৈনিক ক্যাশ রিপোর্ট - " + this.viewModel.getCurrentFormattedDate());
        shareIntent.putExtra("android.intent.extra.TEXT", textSummary + "\n\n(অ্যাপ থেকে পাঠানো দৈনিক হিসাব)");
        if (pdfFile != null && pdfFile.exists()) {
            Uri pdfUri = FileProvider.getUriForFile(this, "com.aistudio.dailycashbook.kxmpzq.fileprovider", pdfFile);
            shareIntent.putExtra("android.intent.extra.STREAM", pdfUri);
            shareIntent.addFlags(1);
        } else {
            shareIntent.setType("text/plain");
        }
        startActivity(Intent.createChooser(shareIntent, "দৈনিক খাতা রিপোর্ট শেয়ার করুন"));
    }

    public void showEditExpenseDialog(final ExpenseModel expense) {
        if (expense == null || isFinishing() || isDestroyed()) {
            return;
        }
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_edit_expense);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        final AutoCompleteTextView etName = dialog.findViewById(R.id.etEditExpenseName);
        final EditText etAmount = dialog.findViewById(R.id.etEditExpenseAmount);
        TextView tvSubtitle = dialog.findViewById(R.id.tvEditExpenseSubtitle);
        Button btnCancel = dialog.findViewById(R.id.btnCancelEdit);
        Button btnSave = dialog.findViewById(R.id.btnSaveEdit);

        if (tvSubtitle != null) {
            tvSubtitle.setText(expense.getDate() + " • " + expense.getTime());
        }

        if (etName != null) {
            etName.setText(expense.getName());
            etName.setSelection(etName.getText().length());

            List<String> rawSuggestions = StorageManager.getInstance(this).getAllProductSuggestionsWithDefaults();
            final List<ExpenseSuggestion> suggestions = new ArrayList<>();
            for (String raw : rawSuggestions) {
                if (raw != null && !raw.trim().isEmpty()) {
                    String cleanName = raw.trim();
                    suggestions.add(new ExpenseSuggestion(getEmojiForProductName(cleanName), cleanName));
                }
            }
            ArrayAdapter<ExpenseSuggestion> autoAdapter = new ArrayAdapter<ExpenseSuggestion>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(suggestions)) {
                private final List<ExpenseSuggestion> originalList = new ArrayList<>(suggestions);

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    if (view instanceof TextView) {
                        ExpenseSuggestion item = getItem(position);
                        if (item != null) {
                            ((TextView) view).setText(item.emoji + "  " + item.name);
                            ((TextView) view).setPadding(24, 20, 24, 20);
                            ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.0f);
                        }
                    }
                    return view;
                }

                @Override
                public Filter getFilter() {
                    return new Filter() {
                        @Override
                        protected FilterResults performFiltering(CharSequence constraint) {
                            FilterResults results = new FilterResults();
                            List<ExpenseSuggestion> filtered = new ArrayList<>();
                            if (constraint == null || constraint.toString().trim().isEmpty()) {
                                filtered.addAll(originalList);
                            } else {
                                String query = constraint.toString().trim().toLowerCase();
                                for (ExpenseSuggestion item : originalList) {
                                    if (item.name.toLowerCase().contains(query)) {
                                        filtered.add(item);
                                    }
                                }
                            }
                            results.values = filtered;
                            results.count = filtered.size();
                            return results;
                        }

                        @Override
                        protected void publishResults(CharSequence constraint, FilterResults results) {
                            clear();
                            if (results != null && results.count > 0 && results.values instanceof List) {
                                addAll((List<ExpenseSuggestion>) results.values);
                            }
                            notifyDataSetChanged();
                        }

                        @Override
                        public CharSequence convertResultToString(Object resultValue) {
                            if (resultValue instanceof ExpenseSuggestion) {
                                return ((ExpenseSuggestion) resultValue).name;
                            }
                            return super.convertResultToString(resultValue);
                        }
                    };
                }
            };
            etName.setAdapter(autoAdapter);
        }

        if (etAmount != null) {
            String amtStr = (expense.getAmount() == (long) expense.getAmount()) ? String.valueOf((long) expense.getAmount()) : String.valueOf(expense.getAmount());
            etAmount.setText(amtStr);
            etAmount.setSelection(etAmount.getText().length());
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String newName = etName != null ? etName.getText().toString().trim() : "";
                    String amtStr = etAmount != null ? etAmount.getText().toString().trim() : "";
                    if (newName.isEmpty()) {
                        Toast.makeText(MainActivity.this, "পণ্যের নাম লিখুন", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (amtStr.isEmpty()) {
                        Toast.makeText(MainActivity.this, "টাকার পরিমাণ লিখুন", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double newAmount = Double.parseDouble(amtStr);
                        if (newAmount <= 0) {
                            Toast.makeText(MainActivity.this, "সঠিক টাকার পরিমাণ দিন", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        boolean updated = MainActivity.this.viewModel.updateExpense(expense.getId(), newName, newAmount);
                        if (updated) {
                            Toast.makeText(MainActivity.this, "খরচ সফলভাবে পরিবর্তন করা হয়েছে", Toast.LENGTH_SHORT).show();
                            MainActivity.this.planAutoCloudBackup();
                            MainActivity.this.setupAutocomplete();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(MainActivity.this, "পরিবর্তন ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, "টাকার পরিমাণ সংখ্যায় লিখুন", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        dialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showDeleteConfirmationDialog(final ExpenseModel expense) {
        new MaterialAlertDialogBuilder(this).setTitle((CharSequence) "খরচ মুছে ফেলবেন?").setMessage((CharSequence) ("আপনি কি নিশ্চিতভাবে \"" + expense.getName() + "\" বাবদ ৳ " + PdfExporter.formatBengaliNumber(expense.getAmount()) + " খরচের হিসাব খতিয়ান থেকে মুছে ফেলতে চান?")).setPositiveButton((CharSequence) "হ্যাঁ", new DialogInterface.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda12
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m7038lambda$showDeleteConfirmationDialog$28$comexampleMainActivity(expense, dialogInterface, i);
            }
        }).setNegativeButton((CharSequence) "না", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showDeleteConfirmationDialog$28$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7038lambda$showDeleteConfirmationDialog$28$comexampleMainActivity(ExpenseModel expense, DialogInterface dialog, int which) {
        this.viewModel.deleteExpense(expense.getId());
        Toast.makeText(this, "হিসাবটি মুছে ফেলা হয়েছে", 0).show();
        planAutoCloudBackup();
    }

    private void showClearAllConfirmationDialog() {
        new MaterialAlertDialogBuilder(this).setTitle((CharSequence) "সব ডিলিট করবেন?").setMessage((CharSequence) "আপনি কি নিশ্চিতভাবে এই খাতার সমস্ত খরচ ও বেচার হিসাব ডিলিট করে নতুন খাতা খুলতে চান? এই কাজটি আর ফিরিয়ে আনা যাবে না।").setPositiveButton((CharSequence) "হ্যাঁ", new DialogInterface.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda8
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m7036xaeb3d55b(dialogInterface, i);
            }
        }).setNegativeButton((CharSequence) "না", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showClearAllConfirmationDialog$29$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7036xaeb3d55b(DialogInterface dialog, int which) {
        this.viewModel.clearAllData();
        this.binding.etSabekCash.setText("");
        this.binding.etAvailableCash.setText("");
        Toast.makeText(this, "খাতার সমস্ত হিসাব মুছে ফেলা হয়েছে", 0).show();
        planAutoCloudBackup();
    }

    private void showDatePickerDialog() {
        Calendar c = Calendar.getInstance();
        if (this.viewModel != null) {
            String activeDateKey = this.viewModel.getActiveDateKey();
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
                Date parsed = sdf.parse(activeDateKey);
                if (parsed != null) {
                    c.setTime(parsed);
                }
            } catch (Exception e) {
            }
        }
        int year = c.get(1);
        int month = c.get(2);
        int day = c.get(5);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda13
            @Override // android.app.DatePickerDialog.OnDateSetListener
            public final void onDateSet(DatePicker datePicker, int i, int i2, int i3) {
                MainActivity.this.m7037lambda$showDatePickerDialog$30$comexampleMainActivity(datePicker, i, i2, i3);
            }
        }, year, month, day);
        datePickerDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showDatePickerDialog$30$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7037lambda$showDatePickerDialog$30$comexampleMainActivity(DatePicker view, int selectedYear, int selectedMonth, int selectedDayOfMonth) {
        this.viewModel.selectDate(selectedYear, selectedMonth, selectedDayOfMonth);
        Toast.makeText(this, "তারিখ পরিবর্তন করে " + selectedDayOfMonth + " মাস " + (selectedMonth + 1) + " করা হয়েছে", 0).show();
    }

    @Override // android.app.Activity
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem clearItem = menu.add(0, 101, 0, "মুছুন");
        clearItem.setIcon(R.drawable.ic_trash);
        try {
            if (clearItem.getIcon() != null) {
                clearItem.getIcon().setTint(Color.parseColor("#EF4444"));
            }
        } catch (Exception e) {
        }
        clearItem.setShowAsAction(2);
        return true;
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 101) {
            showClearAllConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupDashboard() {
        this.binding.tabLayout.addTab(this.binding.tabLayout.newTab().setText("আজকের খাতা").setIcon(R.drawable.ic_notebook));
        this.binding.tabLayout.addTab(this.binding.tabLayout.newTab().setText("সার্বিক ড্যাশবোর্ড").setIcon(R.drawable.ic_dashboard));
        this.binding.tabLayout.addTab(this.binding.tabLayout.newTab().setText("বাকি খাতা").setIcon(R.drawable.ic_notebook));
        this.binding.tabLayout.addTab(this.binding.tabLayout.newTab().setText("ফর্দ খাতা").setIcon(R.drawable.ic_calendar));
        this.binding.tabLayout.addTab(this.binding.tabLayout.newTab().setText("ক্লাউড ব্যাকআপ").setIcon(R.drawable.ic_cloud));
        int[][] states = {new int[]{android.R.attr.state_selected}, new int[]{-16842913}};
        int[] colors = {Color.parseColor("#2563EB"), Color.parseColor("#64748B")};
        this.binding.tabLayout.setTabIconTint(new ColorStateList(states, colors));
        this.binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() { // from class: com.example.MainActivity.5
            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    MainActivity.this.binding.layoutDailyLedger.setVisibility(0);
                    MainActivity.this.binding.layoutDashboard.setVisibility(8);
                    MainActivity.this.binding.layoutBakiKhata.setVisibility(8);
                    MainActivity.this.binding.layoutFordiKhata.setVisibility(8);
                    MainActivity.this.binding.layoutCloudBackup.setVisibility(8);
                    return;
                }
                if (tab.getPosition() == 1) {
                    MainActivity.this.binding.layoutDailyLedger.setVisibility(8);
                    MainActivity.this.binding.layoutDashboard.setVisibility(0);
                    MainActivity.this.binding.layoutBakiKhata.setVisibility(8);
                    MainActivity.this.binding.layoutFordiKhata.setVisibility(8);
                    MainActivity.this.binding.layoutCloudBackup.setVisibility(8);
                    MainActivity.this.updateDashboardUI();
                    return;
                }
                if (tab.getPosition() == 2) {
                    MainActivity.this.binding.layoutDailyLedger.setVisibility(8);
                    MainActivity.this.binding.layoutDashboard.setVisibility(8);
                    MainActivity.this.binding.layoutBakiKhata.setVisibility(0);
                    MainActivity.this.binding.layoutFordiKhata.setVisibility(8);
                    MainActivity.this.binding.layoutCloudBackup.setVisibility(8);
                    MainActivity.this.updateBakiKhataUI();
                    return;
                }
                int position = tab.getPosition();
                MainActivity mainActivity = MainActivity.this;
                if (position == 3) {
                    mainActivity.binding.layoutDailyLedger.setVisibility(8);
                    MainActivity.this.binding.layoutDashboard.setVisibility(8);
                    MainActivity.this.binding.layoutBakiKhata.setVisibility(8);
                    MainActivity.this.binding.layoutFordiKhata.setVisibility(0);
                    MainActivity.this.binding.layoutCloudBackup.setVisibility(8);
                    MainActivity.this.updateFordiKhataUI();
                    return;
                }
                mainActivity.binding.layoutDailyLedger.setVisibility(8);
                MainActivity.this.binding.layoutDashboard.setVisibility(8);
                MainActivity.this.binding.layoutBakiKhata.setVisibility(8);
                MainActivity.this.binding.layoutFordiKhata.setVisibility(8);
                MainActivity.this.binding.layoutCloudBackup.setVisibility(0);
                MainActivity.this.updateCloudBackupUI();
            }

            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        this.binding.btnFilterThisMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDashboardFilterThisMonth = true;
                binding.btnFilterThisMonth.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                binding.btnFilterThisMonth.setTextColor(Color.WHITE);
                binding.btnFilterAllTime.setBackgroundResource(R.drawable.bg_filter_tab_unselected);
                binding.btnFilterAllTime.setTextColor(Color.parseColor("#64748B"));
                updateDashboardUI();
            }
        });
        this.binding.btnFilterAllTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDashboardFilterThisMonth = false;
                binding.btnFilterAllTime.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                binding.btnFilterAllTime.setTextColor(Color.WHITE);
                binding.btnFilterThisMonth.setBackgroundResource(R.drawable.bg_filter_tab_unselected);
                binding.btnFilterThisMonth.setTextColor(Color.parseColor("#64748B"));
                updateDashboardUI();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDashboardUI() {
        double d;
        List<ExpenseModel> rangeExpenses;
        double avgSales;
        double avgSales2;
        double avgSales3;
        List<ExpenseModel> rangeExpenses2;
        String salesStatText;
        String formattedMaxVal;
        String adviseText;
        String matchedCat;
        if (this.viewModel == null || this.binding == null) {
            return;
        }
        List<MainViewModel.DaySummary> summaries = this.viewModel.getHistoricalSummaries();
        String currentMonthSuffix = "";
        String activeDate = this.viewModel.getActiveDateKey();
        if (activeDate.length() >= 10) {
            currentMonthSuffix = activeDate.substring(2);
        }
        boolean thisMonthOnly = this.isDashboardFilterThisMonth;
        List<MainViewModel.DaySummary> filtered = new ArrayList<>();
        double totalSalesSum = 0.0d;
        double totalExpSum = 0.0d;
        for (MainViewModel.DaySummary ds : summaries) {
            if (!thisMonthOnly || currentMonthSuffix.isEmpty() || ds.dateKey.endsWith(currentMonthSuffix)) {
                filtered.add(ds);
                totalSalesSum += ds.computedSale;
                totalExpSum += ds.expenses;
            }
        }
        double netProfitSum = totalSalesSum - totalExpSum;
        this.binding.tvDashTotalSales.setText("৳ " + PdfExporter.formatBengaliNumber(totalSalesSum));
        this.binding.tvDashTotalExpenses.setText("৳ " + PdfExporter.formatBengaliNumber(totalExpSum));
        this.binding.tvDashNetProfit.setText("৳ " + PdfExporter.formatBengaliNumber(Math.abs(netProfitSum)));
        if (netProfitSum > 0.0d) {
            d = 0.0d;
            this.binding.cardDashNetStatus.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#ECFDF5")));
            this.binding.tvDashNetPrefix.setText("সার্বিকভাবে নিট লাভ");
            this.binding.tvDashNetPrefix.setTextColor(Color.parseColor("#047857"));
            this.binding.tvDashNetProfit.setTextColor(Color.parseColor("#047857"));
            this.binding.ivDashNetIcon.setImageResource(R.drawable.ic_notebook);
            this.binding.ivDashNetIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#047857")));
            this.binding.ivDashNetIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D1FAE5")));
        } else {
            d = 0.0d;
            ActivityMainBinding activityMainBinding = this.binding;
            if (netProfitSum < 0.0d) {
                activityMainBinding.cardDashNetStatus.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FFF1F2")));
                this.binding.tvDashNetPrefix.setText("সার্বিকভাবে নিট ঘাটতি");
                this.binding.tvDashNetPrefix.setTextColor(Color.parseColor("#BE123C"));
                this.binding.tvDashNetProfit.setTextColor(Color.parseColor("#BE123C"));
                this.binding.ivDashNetIcon.setImageResource(R.drawable.ic_trash);
                this.binding.ivDashNetIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#BE123C")));
                this.binding.ivDashNetIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFE4E6")));
            } else {
                activityMainBinding.cardDashNetStatus.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F8FAFC")));
                this.binding.tvDashNetPrefix.setText("সার্বিকভাবে হিসাব সমান");
                this.binding.tvDashNetPrefix.setTextColor(Color.parseColor("#475569"));
                this.binding.tvDashNetProfit.setTextColor(Color.parseColor("#475569"));
                this.binding.ivDashNetIcon.setImageResource(R.drawable.ic_notebook);
                this.binding.ivDashNetIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#475569")));
                this.binding.ivDashNetIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));
            }
        }
        int progressValue = 0;
        if (totalSalesSum > d) {
            progressValue = (int) Math.round((totalExpSum / totalSalesSum) * 100.0d);
        }
        if (progressValue > 100) {
            progressValue = 100;
        }
        if (progressValue < 0) {
            progressValue = 0;
        }
        this.binding.progressRatioBar.setProgress(progressValue);
        this.binding.tvDashProgressPercent.setText(PdfExporter.formatBengaliNumber(progressValue) + "% খরচ");
        int remainingValue = 100 - progressValue;
        this.binding.tvDashProgressRemainingPercent.setText(PdfExporter.formatBengaliNumber(remainingValue) + "% লাভ");
        List<ExpenseModel> rangeExpenses3 = new ArrayList<>();
        StorageManager storage = StorageManager.getInstance(getApplication());
        Iterator<MainViewModel.DaySummary> it = filtered.iterator();
        while (it.hasNext()) {
            rangeExpenses3.addAll(storage.loadExpenses(it.next().dateKey));
            remainingValue = remainingValue;
        }
        this.binding.pieChartView.setExpenses(rangeExpenses3);
        this.binding.lineGraphView.setData(filtered);
        if (filtered.isEmpty()) {
            rangeExpenses = rangeExpenses3;
            avgSales = d;
        } else {
            rangeExpenses = rangeExpenses3;
            avgSales = totalSalesSum / filtered.size();
        }
        if (filtered.isEmpty()) {
            avgSales2 = avgSales;
            avgSales3 = d;
        } else {
            avgSales2 = avgSales;
            double avgSales4 = filtered.size();
            avgSales3 = totalExpSum / avgSales4;
        }
        if (filtered.isEmpty()) {
            salesStatText = "• বেচা বনাম খরচ: কোনো রেকর্ড খুঁজে পাওয়া যায়নি। দৈনিক খাতা এন্ট্রি করুন।";
            rangeExpenses2 = rangeExpenses;
        } else {
            double avgExp = avgSales3;
            String formattedAvgSales = PdfExporter.formatBengaliNumber((int) Math.round(avgSales2));
            String formattedAvgExp = PdfExporter.formatBengaliNumber((int) Math.round(avgExp));
            if (avgSales2 > avgExp) {
                rangeExpenses2 = rangeExpenses;
                salesStatText = "• বেচা বনাম খরচ: দিনপ্রতি গড় বিক্রি ৳ " + formattedAvgSales + " যা গড় খরচ ৳ " + formattedAvgExp + " এর তুলনায় বেশি। এটি ইতিবাচক মুনাফা নির্দেশক।";
            } else {
                rangeExpenses2 = rangeExpenses;
                if (avgSales2 < avgExp) {
                    salesStatText = "• বেচা বনাম খরচ: গড় খরচ ৳ " + formattedAvgExp + " এবং গড় বিক্রি ৳ " + formattedAvgSales + "। ব্যবসা প্রবৃদ্ধ করতে খরচ নিয়ন্ত্রণ করা প্রয়োজন।";
                } else {
                    salesStatText = "• বেচা বনাম খরচ: গড় বিক্রি ও গড় খরচ উভয়ই ৳ " + formattedAvgSales + " মূল্যে সমান রয়েছে। ব্রেক-ইভেন স্তর বজায় রয়েছে।";
                }
            }
        }
        this.binding.tvAnalysisSaleStat.setText(salesStatText);
        Map<String, Double> categoryTotals = new HashMap<>();
        categoryTotals.put("🛍️ বাজার", Double.valueOf(d));
        categoryTotals.put("🏠 ভাড়া", Double.valueOf(d));
        categoryTotals.put("🚌 পরিবহন", Double.valueOf(d));
        categoryTotals.put("💊 ওষুধ", Double.valueOf(d));
        categoryTotals.put("🏦 ব্যাংক", Double.valueOf(d));
        categoryTotals.put("🌾 কাঁচামাল", Double.valueOf(d));
        categoryTotals.put("⚙️ অন্যান্য", Double.valueOf(d));
        Iterator<ExpenseModel> it2 = rangeExpenses2.iterator();
        while (it2.hasNext()) {
            ExpenseModel exp = it2.next();
            String name = exp.getName() != null ? exp.getName().trim() : "";
            if (name.isEmpty()) {
                continue;
            }
            Iterator<ExpenseModel> it3 = it2;
            String nameLower = name.toLowerCase();
            int progressValue2 = progressValue;
            if (nameLower.contains("বাজার") || nameLower.contains("চাল") || nameLower.contains("আটা") || nameLower.contains("ডাল") || nameLower.contains("তেল")) {
                matchedCat = "🛍️ বাজার";
            } else if (nameLower.contains("ভাড়া") || nameLower.contains("ভাড়া") || nameLower.contains("মেস") || nameLower.contains("দোকান") || nameLower.contains("বাড়ি")) {
                matchedCat = "🏠 ভাড়া";
            } else if (nameLower.contains("পরিবহন") || nameLower.contains("বাস") || nameLower.contains("রিকশা") || nameLower.contains("ভ্যান") || nameLower.contains("যাতায়াত") || nameLower.contains("গাড়ি")) {
                matchedCat = "🚌 পরিবহন";
            } else if (nameLower.contains("ওষুধ") || nameLower.contains("ঔষধ") || nameLower.contains("ডাক্তার") || nameLower.contains("মেডিকেল") || nameLower.contains("হাসপাতাল")) {
                matchedCat = "💊 ওষুধ";
            } else if (nameLower.contains("ব্যাংক") || nameLower.contains("রকেট") || nameLower.contains("বিকাশ") || nameLower.contains("নগদ") || nameLower.contains("সার্ভিস") || nameLower.contains("ট্যাক্স")) {
                matchedCat = "🏦 ব্যাংক";
            } else if (nameLower.contains("কাঁচামাল") || nameLower.contains("সবজি") || nameLower.contains("ফল") || nameLower.contains("মাছ") || nameLower.contains("মাংস") || nameLower.contains("ডিম")) {
                matchedCat = "🌾 কাঁচামাল";
            } else {
                matchedCat = name;
            }
            if (!categoryTotals.containsKey(matchedCat)) {
                categoryTotals.put(matchedCat, Double.valueOf(d));
            }
            categoryTotals.put(matchedCat, Double.valueOf(categoryTotals.get(matchedCat).doubleValue() + exp.getAmount()));
            it2 = it3;
            progressValue = progressValue2;
        }
        String maxCategory = "⚙️ অন্যান্য";
        double maxCatVal = 0.0d;
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            if (entry.getValue().doubleValue() > maxCatVal) {
                maxCatVal = entry.getValue().doubleValue();
                String maxCategory2 = entry.getKey();
                maxCategory = maxCategory2;
            }
        }
        if (maxCatVal == d) {
            formattedMaxVal = "• সর্বোচ্চ ব্যয়ের খাত: এখনও কোনো খরচের রেকর্ড পাওয়া যায়নি।";
        } else {
            String formattedMaxVal2 = PdfExporter.formatBengaliNumber((int) Math.round(maxCatVal));
            formattedMaxVal = "• সর্বোচ্চ ব্যয়ের খাত: \"" + maxCategory + "\" খাতে সর্বোচ্চ ব্যয় হয়েছে (মোট ৳ " + formattedMaxVal2 + ")। এই খাতে সচেতন বাজেট মেলাতে পারেন।";
        }
        this.binding.tvAnalysisHighestExpense.setText(formattedMaxVal);
        if (maxCategory.equals("🛍️ বাজার")) {
            adviseText = "• লাভ সাশ্রয়ী পরামর্শ: বাজার ক্রয়ে অপচয় কমাতে এবং অর্থ সাশ্রয় করতে চাল, ডাল ও তেল পাইকারি বাজার হতে বড় মাপে একবারে ক্রয়ের অভ্যাস করুন।";
        } else if (maxCategory.equals("🌾 কাঁচামাল")) {
            adviseText = "• লাভ সাশ্রয়ী পরামর্শ: কাঁচামাল সরাসরি আড়ত বা চুক্তিভিত্তিক চাষীদের নিকট হতে সংগ্রহ করতে পারলে উৎপাদন মূল্যে প্রায় ১০-১৫% সাশ্রয় আনা সম্ভব।";
        } else if (maxCategory.equals("⚙️ অন্যান্য")) {
            adviseText = "• লাভ সাশ্রয়ী পরামর্শ: ফুটকর ও বিবিধ অন্যান্য খরচগুলো সবসময় নিখুঁতভাবে লিখে রাখুন; এটি অপ্রয়োজনীয় ছোট ছোট অপব্যয় সনাক্ত করতে সহায়তা করবে।";
        } else if (netProfitSum < d) {
            adviseText = "• লাভ সাশ্রয়ী পরামর্শ: ড্যাশবোর্ডে ঘাটতি রয়েছে। নতুন কোনো ব্যয়ের পূর্বে সাবেক ক্যাশ তহবিলের উদ্বৃত্ত পুনঃমূল্যায়ন করে লাভ বৃদ্ধি করুন।";
        } else {
            adviseText = "• লাভ সাশ্রয়ী পরামর্শ: অর্জিত নিট মুনাফা সাবেক ক্যাশের সাথে যুক্ত করুন এবং অপ্রয়োজনীয় নগদ উত্তোলন এড়াতে একটি নির্দিষ্ট সাপ্তাহিক বাজেট মেনে চলুন।";
        }
        this.binding.tvAnalysisAdvise.setText(adviseText);
        this.binding.layoutDashHistoryList.removeAllViews();
        boolean isEmpty = filtered.isEmpty();
        ActivityMainBinding activityMainBinding2 = this.binding;
        if (isEmpty) {
            activityMainBinding2.layoutDashHistoryEmpty.setVisibility(0);
            return;
        }
        activityMainBinding2.layoutDashHistoryEmpty.setVisibility(8);
        Iterator<MainViewModel.DaySummary> it4 = filtered.iterator();
        while (it4.hasNext()) {
            final MainViewModel.DaySummary ds2 = it4.next();
            Map<String, Double> categoryTotals2 = categoryTotals;
            String maxCategory3 = maxCategory;
            View row = getLayoutInflater().inflate(R.layout.item_dashboard_history, (ViewGroup) this.binding.layoutDashHistoryList, false);
            TextView tvDate = (TextView) row.findViewById(R.id.tvHistoryDate);
            TextView tvDay = (TextView) row.findViewById(R.id.tvHistoryDay);
            TextView tvPill = (TextView) row.findViewById(R.id.tvHistoryStatusPill);
            String adviseText2 = adviseText;
            TextView tvSales = (TextView) row.findViewById(R.id.tvHistorySales);
            Iterator<MainViewModel.DaySummary> it5 = it4;
            TextView tvExpenses = (TextView) row.findViewById(R.id.tvHistoryExpenses);
            String highestExpenseText = formattedMaxVal;
            View card = row.findViewById(R.id.cardHistoryRow);
            double totalExpSum2 = totalExpSum;
            tvDate.setText(ds2.dateKey);
            tvDay.setText(getBengaliDayFromDateKey(ds2.dateKey));
            tvSales.setText("৳ " + PdfExporter.formatBengaliNumber(ds2.computedSale));
            tvExpenses.setText("৳ " + PdfExporter.formatBengaliNumber(ds2.expenses));
            if (ds2.margin > d) {
                tvPill.setText("🟢 লাভ ৳ " + PdfExporter.formatBengaliNumber(ds2.margin));
                tvPill.setTextColor(Color.parseColor("#16A34A"));
                tvPill.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DCFCE7")));
            } else if (ds2.margin < d) {
                tvPill.setText("🔴 ঘাটতি ৳ " + PdfExporter.formatBengaliNumber(Math.abs(ds2.margin)));
                tvPill.setTextColor(Color.parseColor("#DC2626"));
                tvPill.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
            } else {
                tvPill.setText("✅ সমান");
                tvPill.setTextColor(Color.parseColor("#059669"));
                tvPill.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ECFDF5")));
            }
            card.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda3
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MainActivity.this.m7046lambda$updateDashboardUI$32$comexampleMainActivity(ds2, view);
                }
            });
            this.binding.layoutDashHistoryList.addView(row);
            categoryTotals = categoryTotals2;
            maxCategory = maxCategory3;
            adviseText = adviseText2;
            it4 = it5;
            formattedMaxVal = highestExpenseText;
            totalExpSum = totalExpSum2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateDashboardUI$32$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7046lambda$updateDashboardUI$32$comexampleMainActivity(MainViewModel.DaySummary ds, View v) {
        String[] parts = ds.dateKey.split("-");
        if (parts.length == 3) {
            try {
                int d = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]) - 1;
                int y = Integer.parseInt(parts[2]);
                this.viewModel.selectDate(y, m, d);
                TabLayout.Tab tab = this.binding.tabLayout.getTabAt(0);
                if (tab != null) {
                    tab.select();
                }
                Toast.makeText(this, ds.dateKey + " তারিখের হিসাব খোলা হয়েছে", 0).show();
            } catch (Exception e) {
            }
        }
    }

    private String getBengaliDayFromDateKey(String dateKey) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
            Date parsed = sdf.parse(dateKey);
            if (parsed != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(parsed);
                int day = cal.get(7);
                switch (day) {
                    case 1:
                        return " (রবিবার)";
                    case 2:
                        return " (সোমবার)";
                    case 3:
                        return " (মঙ্গলবার)";
                    case 4:
                        return " (বুধবার)";
                    case 5:
                        return " (বৃহস্পতিবার)";
                    case 6:
                        return " (শুক্রবার)";
                    case 7:
                        return " (শনিবার)";
                    default:
                        return "";
                }
            }
        } catch (Exception e) {
        }
        return "";
    }

    public void updateCloudBackupUI() {
        setupCloudBackup();
    }

    private void setupCloudBackup() {
        setupGoogleSheetsSync();
        updateUserProfileHeader();
    }

    private void updateUserProfileHeader() {
        if (this.binding == null || this.binding.tvUserProfileHeaderName == null) return;
        this.binding.tvUserProfileHeaderName.setText("মাওয়া স্টোর");
        this.binding.ivUserProfileHeaderIcon.setImageResource(R.drawable.ic_cloud);
        this.binding.ivUserProfileHeaderIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#059669")));
        this.binding.btnUserProfileHeader.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ECFDF5")));
        this.binding.btnUserProfileHeader.setOnClickListener(v -> {
            if (this.binding.layoutCloudBackup.getVisibility() == View.VISIBLE) {
                this.binding.layoutCloudBackup.setVisibility(View.GONE);
                this.binding.layoutDailyLedger.setVisibility(View.VISIBLE);
                this.binding.tabLayout.selectTab(this.binding.tabLayout.getTabAt(0));
            } else {
                this.binding.layoutDailyLedger.setVisibility(View.GONE);
                this.binding.layoutDashboard.setVisibility(View.GONE);
                this.binding.layoutBakiKhata.setVisibility(View.GONE);
                this.binding.layoutFordiKhata.setVisibility(View.GONE);
                this.binding.layoutCloudBackup.setVisibility(View.VISIBLE);
                this.binding.tabLayout.selectTab(this.binding.tabLayout.getTabAt(4));
            }
        });
    }

    private void setupGoogleSheetsSync() {
        final GoogleSheetsSyncManager sheetsSyncManager = GoogleSheetsSyncManager.getInstance(this);
        this.binding.etGoogleSpreadsheetId.setText(sheetsSyncManager.getSpreadsheetId());
        this.binding.etGoogleSheetGid.setText(sheetsSyncManager.getSheetGid());
        this.binding.etGoogleSheetsUrl.setText(sheetsSyncManager.getSheetsUrl());

        if (sheetsSyncManager.isConnected()) {
            this.binding.tvLastSheetsSyncTime.setText("✅ মাওয়া স্টোর গুগল শিট সফলভাবে কনফিগার করা আছে");
        } else {
            this.binding.tvLastSheetsSyncTime.setText("সর্বশেষ সিঙ্ক: এখনো সিঙ্ক করা হয়নি");
        }

        this.binding.btnSaveSheetsUrl.setOnClickListener(v -> {
            String spreadsheetIdOrUrl = this.binding.etGoogleSpreadsheetId.getText() != null ? this.binding.etGoogleSpreadsheetId.getText().toString().trim() : "";
            String sheetGid = this.binding.etGoogleSheetGid.getText() != null ? this.binding.etGoogleSheetGid.getText().toString().trim() : "0";
            String webAppUrl = this.binding.etGoogleSheetsUrl.getText() != null ? this.binding.etGoogleSheetsUrl.getText().toString().trim() : "";

            if (spreadsheetIdOrUrl.isEmpty() && webAppUrl.isEmpty()) {
                Toast.makeText(this, "⚠️ দয়া করে গুগল স্প্রেডশিট আইডি বা লিংক দিন।", Toast.LENGTH_SHORT).show();
                return;
            }

            sheetsSyncManager.saveSheetConfig(spreadsheetIdOrUrl, sheetGid);
            if (!webAppUrl.isEmpty()) {
                sheetsSyncManager.saveSheetsUrl(webAppUrl);
            }

            this.binding.etGoogleSpreadsheetId.setText(sheetsSyncManager.getSpreadsheetId());
            this.binding.etGoogleSheetGid.setText(sheetsSyncManager.getSheetGid());
            this.binding.tvLastSheetsSyncTime.setText("✅ সেটিংস সফলভাবে সংরক্ষণ করা হয়েছে!");
            Toast.makeText(this, "✅ মাওয়া স্টোর গুগল শিট সেটিংস সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show();
        });

        this.binding.btnOpenGoogleSheet.setOnClickListener(v -> {
            String sheetUrl = sheetsSyncManager.getSpreadsheetUrl();
            if (sheetUrl.isEmpty()) {
                Toast.makeText(this, "⚠️ আগে স্প্রেডশিট আইডি দিয়ে সেভ করুন!", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sheetUrl));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "ব্রাউজারে শিট খুলতে ব্যর্থ: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        this.binding.btnSyncToSheetsNow.setOnClickListener(v -> {
            if (!sheetsSyncManager.isConnected()) {
                Toast.makeText(this, "⚠️ আগে গুগল স্প্রেডশিট আইডি অথবা ওয়েব অ্যাপ লিংক দিন!", Toast.LENGTH_LONG).show();
                return;
            }
            this.binding.progressCloudAction.setVisibility(View.VISIBLE);
            this.binding.btnSyncToSheetsNow.setEnabled(false);
            this.binding.btnCloudRestore.setEnabled(false);

            sheetsSyncManager.syncData(this, new GoogleSheetsSyncManager.SyncCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        binding.progressCloudAction.setVisibility(View.GONE);
                        binding.btnSyncToSheetsNow.setEnabled(true);
                        binding.btnCloudRestore.setEnabled(true);
                        binding.tvLastSheetsSyncTime.setText("সর্বশেষ সিঙ্ক: সফল (" + new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date()) + ")");
                        new MaterialAlertDialogBuilder(MainActivity.this)
                                .setTitle("🎉 সিঙ্ক সফল!")
                                .setMessage(message + "\n\nমাওয়া স্টোর এর সকল হিসাব গুগল শিটে নিরাপদে সংরক্ষণ করা হয়েছে।")
                                .setPositiveButton("ঠিক আছে", null)
                                .show();
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        binding.progressCloudAction.setVisibility(View.GONE);
                        binding.btnSyncToSheetsNow.setEnabled(true);
                        binding.btnCloudRestore.setEnabled(true);
                        new MaterialAlertDialogBuilder(MainActivity.this)
                                .setTitle("❌ সিঙ্ক ব্যর্থ হয়েছে")
                                .setMessage(error + "\n\n💡 সমাধান: স্প্রেডশিট আইডি বা Apps Script URL সঠিক আছে কিনা এবং শিটের এক্সেস 'Anyone with link' করা রয়েছে কিনা চেক করুন।")
                                .setPositiveButton("ঠিক আছে", null)
                                .show();
                    });
                }
            });
        });

        this.binding.btnCloudRestore.setOnClickListener(v -> {
            if (!sheetsSyncManager.isConnected()) {
                Toast.makeText(this, "⚠️ আগে গুগল স্প্রেডশিট আইডি বা গিড (GID) সেট করুন!", Toast.LENGTH_LONG).show();
                return;
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle("গুগল শিট থেকে রিস্টোর")
                    .setMessage("গুগল শিট থেকে ডাটা রিস্টোর করলে অ্যাপে শিটের সংরক্ষিত হিসাবসমূহ লোড হবে। আপনি কি রিস্টোর করতে চান?")
                    .setPositiveButton("হ্যাঁ, রিস্টোর করুন", (dialog, which) -> {
                        binding.progressCloudAction.setVisibility(View.VISIBLE);
                        binding.btnSyncToSheetsNow.setEnabled(false);
                        binding.btnCloudRestore.setEnabled(false);

                        sheetsSyncManager.restoreFromGoogleSheet(this, new GoogleSheetsSyncManager.DataCallback() {
                            @Override
                            public void onSuccess(Map<String, Object> data) {
                                runOnUiThread(() -> {
                                    binding.progressCloudAction.setVisibility(View.GONE);
                                    binding.btnSyncToSheetsNow.setEnabled(true);
                                    binding.btnCloudRestore.setEnabled(true);
                                    if (data != null && !data.isEmpty()) {
                                        StorageManager.getInstance(MainActivity.this).importAllData(data);
                                        viewModel.loadSavedData();
                                        updateDashboardUI();
                                        binding.tvLastSheetsSyncTime.setText("সর্বশেষ রিস্টোর: সফল (" + new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date()) + ")");
                                        new MaterialAlertDialogBuilder(MainActivity.this)
                                                .setTitle("✅ রিস্টোর সফল!")
                                                .setMessage("গুগল শিট থেকে মাওয়া স্টোর এর সকল হিসাব সফলভাবে অ্যাপে পুনরুদ্ধার করা হয়েছে।")
                                                .setPositiveButton("ঠিক আছে", null)
                                                .show();
                                    } else {
                                        Toast.makeText(MainActivity.this, "শিটে কোনো ডাটা পাওয়া যায়নি!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onFailure(String error) {
                                runOnUiThread(() -> {
                                    binding.progressCloudAction.setVisibility(View.GONE);
                                    binding.btnSyncToSheetsNow.setEnabled(true);
                                    binding.btnCloudRestore.setEnabled(true);
                                    new MaterialAlertDialogBuilder(MainActivity.this)
                                            .setTitle("❌ রিস্টোর ব্যর্থ")
                                            .setMessage(error)
                                            .setPositiveButton("ঠিক আছে", null)
                                            .show();
                                });
                            }
                        });
                    })
                    .setNegativeButton("বাতিল", null)
                    .show();
        });

        this.binding.btnSheetsInstruction.setOnClickListener(v -> {
            final String scriptCode = "function doPost(e) {\n  try {\n    var json = JSON.parse(e.postData.contents);\n    var ss = SpreadsheetApp.getActiveSpreadsheet();\n    var ledgerSheet = ss.getSheetByName(\"Daily Cash Book\");\n    if (!ledgerSheet) {\n      ledgerSheet = ss.insertSheet(\"Daily Cash Book\");\n    }\n    ledgerSheet.clear();\n    \n    var headers = [\"তারিখ (Date)\", \"সাবেক ক্যাশ (Opening Cash)\", \"মোট খরচ (Total Expenses)\", \"মোট বেচা (Total Sale)\", \"হাতে থাকা ক্যাশ (Cash in Hand)\", \"লাভ / ঘাটতি (Profit/Loss)\"];\n    ledgerSheet.getRange(1, 1, 1, headers.length).setValues([headers]).setFontWeight(\"bold\").setBackground(\"#D1FAE5\");\n    \n    var rows = [];\n    if (json.summaries && json.summaries.length > 0) {\n      for (var i = 0; i < json.summaries.length; i++) {\n        var s = json.summaries[i];\n        rows.push([\n          s.dateKey,\n          s.sabekCash,\n          s.expenses,\n          s.computedSale,\n          s.availableCash,\n          s.profitOrLoss\n        ]);\n      }\n    }\n    \n    if (rows.length > 0) {\n      ledgerSheet.getRange(2, 1, rows.length, headers.length).setValues(rows);\n    }\n    ledgerSheet.autoResizeColumns(1, headers.length);\n    return ContentService.createTextOutput(JSON.stringify({ \n      status: \"success\", \n      message: \"সফলভাবে \" + rows.length + \" দিনের ডাটা সিঙ্ক হয়েছে!\" \n    })).setMimeType(ContentService.MimeType.JSON);\n  } catch (error) {\n    return ContentService.createTextOutput(JSON.stringify({ \n      status: \"error\", \n      message: error.toString() \n    })).setMimeType(ContentService.MimeType.JSON);\n  }\n}";

            ScrollView scrollView = new ScrollView(this);
            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(40, 30, 40, 30);

            TextView txtTitle = new TextView(this);
            txtTitle.setText("📊 মাওয়া স্টোর - গুগল শিট সেটআপ গাইড");
            txtTitle.setTextSize(16.0f);
            txtTitle.setTypeface(null, 1);
            txtTitle.setTextColor(Color.parseColor("#047857"));
            txtTitle.setPadding(0, 0, 0, 16);

            TextView txtSteps = new TextView(this);
            txtSteps.setText("১. একটি গুগল শিট (Google Sheet) তৈরি করুন অথবা বিদ্যমান শিট খুলুন।\n\n২. শিটের শেয়ারিং অপশনে গিয়ে 'Anyone with the link can view' (অথবা Viewer/Editor) এক্সেস দিন।\n\n৩. শিটের লিংক অথবা লিঙ্ক থেকে Spreadsheet ID এবং Sheet GID (যেমন: 0) কপি করে অ্যাপের বক্সে বসান।\n\n৪. (ঐচ্ছিক - ফুল অটো সিঙ্ক): শিটের Extensions -> Apps Script এ গিয়ে নিচের কোডটি পেস্ট করে Web app হিসেবে Deploy করুন (Access: Anyone)। সেই URL টি অ্যাপে বসিয়ে দিন।");
            txtSteps.setTextSize(13.0f);
            txtSteps.setTextColor(Color.parseColor("#334155"));
            txtSteps.setLineSpacing(3.0f, 1.15f);
            txtSteps.setPadding(0, 0, 0, 20);

            Button btnCopy = new Button(this);
            btnCopy.setText("📋 Apps Script কোড কপি করুন");
            btnCopy.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#059669")));
            btnCopy.setTextColor(Color.WHITE);
            btnCopy.setOnClickListener(btn -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Google Sheets Apps Script", scriptCode);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "✅ কোড ক্লিপবোর্ডে কপি করা হয়েছে!", Toast.LENGTH_SHORT).show();
            });

            container.addView(txtTitle);
            container.addView(txtSteps);
            container.addView(btnCopy);
            scrollView.addView(container);

            new MaterialAlertDialogBuilder(this)
                    .setView(scrollView)
                    .setPositiveButton("ঠিক আছে, বুঝলাম", null)
                    .show();
        });
    }

    private void setupLocalBackup() {
        this.binding.btnLocalBackupSave.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "mawa_store_backup.json");
                startActivityForResult(intent, 2001);
            } catch (Exception e) {
                Toast.makeText(this, "ব্যাকআপ উইন্ডো খুলতে ব্যর্থ: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        this.binding.btnLocalBackupShare.setOnClickListener(v -> {
            try {
                Map<String, Object> data = StorageManager.getInstance(this).exportAllData();
                String json = new Gson().toJson(data);
                File cacheDir = getCacheDir();
                File backupFile = new File(cacheDir, "mawa_store_backup.json");
                FileWriter writer = new FileWriter(backupFile);
                writer.write(json);
                writer.flush();
                writer.close();
                Uri fileUri = FileProvider.getUriForFile(this, "com.aistudio.dailycashbook.kxmpzq.fileprovider", backupFile);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/json");
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, "মাওয়া স্টোর ব্যাকআপ ফাইল শেয়ার করুন"));
            } catch (Exception e) {
                Toast.makeText(this, "শেয়ার করতে ব্যর্থ: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        this.binding.btnLocalBackupRestore.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("হিসাব রিস্টোর করুন")
                    .setMessage("পুনরুদ্ধার বা রিস্টোর করার সময় আপনার অ্যাপের বর্তমান সব ডাটা মুছে গিয়ে রিস্টোরকৃত ফাইলের ডাটা দিয়ে প্রতিস্থাপিত হবে। আপনি কি রিস্টোর করতে চান?")
                    .setPositiveButton("হ্যাঁ, রিস্টোর করুন", (dialog, which) -> {
                        try {
                            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.setType("application/json");
                            startActivityForResult(intent, 2002);
                        } catch (Exception e) {
                            Toast.makeText(this, "ফাইল উইন্ডো খুলতে ব্যর্থ: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("বাতিল", null)
                    .show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2001 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                String json = new Gson().toJson(StorageManager.getInstance(this).exportAllData());
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    outputStream.write(json.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    outputStream.close();
                    Toast.makeText(this, "অভিনন্দন! মাওয়া স্টোরের লোকাল ব্যাকআপ ফাইলটি সংরক্ষিত হয়েছে।", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "ব্যাকআপ ফাইলে লিখতে অনুমতি দেয়া হয়নি।", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "ব্যাকআপ ফাইল সংরক্ষণ ব্যর্থ: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == 2002 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri2 = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri2);
                if (inputStream != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    inputStream.close();
                    String json2 = sb.toString();
                    Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
                    Map<String, Object> backupData = new Gson().fromJson(json2, mapType);
                    if (backupData != null) {
                        StorageManager.getInstance(this).importAllData(backupData);
                        this.viewModel.loadSavedData();
                        updateDashboardUI();
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("লোকাল রিস্টোর সফল")
                                .setMessage("লোকাল ব্যাকআপ ফাইল থেকে মাওয়া স্টোর এর সকল হিসাব সুন্দরভাবে পুনরুদ্ধার সম্পন্ন হয়েছে!")
                                .setPositiveButton("ঠিক আছে", null)
                                .show();
                        return;
                    }
                    Toast.makeText(this, "ব্যাকআপ ফাইলের ফরম্যাট সঠিক নয়!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e2) {
                Toast.makeText(this, "লোকাল রিস্টোর ব্যর্থ: " + e2.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void planAutoCloudBackup() {
        this.backupHandler.removeCallbacks(this.backupRunnable);
        this.backupHandler.postDelayed(this.backupRunnable, 2000L);
    }

    public void triggerAutoCloudBackup() {
        GoogleSheetsSyncManager sheetsSyncManager = GoogleSheetsSyncManager.getInstance(this);
        if (sheetsSyncManager.isConnected()) {
            sheetsSyncManager.syncData(this, new GoogleSheetsSyncManager.SyncCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        if (binding != null && binding.tvLastSheetsSyncTime != null) {
                            binding.tvLastSheetsSyncTime.setText("সর্বশেষ সিঙ্ক: " + new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date()));
                        }
                    });
                }

                @Override
                public void onFailure(String error) {
                }
            });
        }
    }


    private void applySuggestedSabekCash() {
        if (this.viewModel == null || this.binding == null) {
            return;
        }
        double suggested = this.viewModel.getSuggestedSabekCash();
        if (suggested > 0.0d) {
            String strVal = String.valueOf(suggested);
            if (strVal.endsWith(".0")) {
                strVal = strVal.substring(0, strVal.length() - 2);
            }
            this.binding.etSabekCash.setText(strVal);
            this.viewModel.setSabekCash(suggested);
            this.binding.btnSuggestSabekCash.setVisibility(View.GONE);
            Toast.makeText(this, "সাবেক হিসেবে গতকালকের ক্যাশ ৳ " + PdfExporter.formatBengaliNumber(suggested) + " গ্রহণ করা হয়েছে", Toast.LENGTH_SHORT).show();
            planAutoCloudBackup();
        }
    }

    private void updateSabekSuggestionUI() {
        if (this.binding == null || this.viewModel == null) {
            return;
        }
        double suggested = this.viewModel.getSuggestedSabekCash();
        double currentSabek = this.viewModel.getSabekCash().getValue() != null ? this.viewModel.getSabekCash().getValue().doubleValue() : 0.0d;
        if (suggested > 0.0d && currentSabek == 0.0d) {
            this.binding.tvSabekSuggestionText.setText("গতকালকের সমাপনী ক্যাশ: ৳ " + PdfExporter.formatBengaliNumber(suggested));
            this.binding.btnSuggestSabekCash.setVisibility(View.VISIBLE);
        } else {
            this.binding.btnSuggestSabekCash.setVisibility(View.GONE);
        }
    }

    public static String getEmojiForProductName(String name) {
        if (name == null || name.isEmpty()) {
            return "📦";
        }
        String lower = name.toLowerCase();
        if (lower.contains("চাল") || lower.contains("ভাত") || lower.contains("ধান")) {
            return "🍚";
        }
        if (lower.contains("আটা") || lower.contains("ময়দা") || lower.contains("সুজি")) {
            return "🌾";
        }
        if (lower.contains("তেল") || lower.contains("ঘি") || lower.contains("সয়াবিন")) {
            return "🛢️";
        }
        if (lower.contains("লবণ") || lower.contains("লবন")) {
            return "🧂";
        }
        if (lower.contains("চিনি") || lower.contains("গুড়")) {
            return "🍬";
        }
        if (lower.contains("ডিম")) {
            return "🥚";
        }
        if (lower.contains("দুধ") || lower.contains("মিল্ক")) {
            return "🥛";
        }
        if (lower.contains("চা পাতা") || lower.contains("চা") || lower.contains("কফি")) {
            return "🍃";
        }
        if (lower.contains("ডাল") || lower.contains("ছোলা") || lower.contains("বুট")) {
            return "🍲";
        }
        if (lower.contains("পেঁয়াজ") || lower.contains("পিঁয়াজ") || lower.contains("রসুন") || lower.contains("আদা")) {
            return "🧅";
        }
        if (lower.contains("আলু")) {
            return "🥔";
        }
        if (lower.contains("মরিচ") || lower.contains("মসলা") || lower.contains("হলুদ") || lower.contains("ধনে")) {
            return "🌶️";
        }
        if (lower.contains("বিস্কুট") || lower.contains("বিস্ক") || lower.contains("কুকিজ")) {
            return "🍪";
        }
        if (lower.contains("চকলেট") || lower.contains("চকো")) {
            return "🍫";
        }
        if (lower.contains("পাউরুটি") || lower.contains("বন") || lower.contains("টোস্ট") || lower.contains("কেক") || lower.contains("বেকারি")) {
            return "🍞";
        }
        if (lower.contains("আইস") || lower.contains("ললি") || lower.contains("আইসক্রিম")) {
            return "🍧";
        }
        if (lower.contains("জুস") || lower.contains("লাবাং") || lower.contains("বোরহানি")) {
            return "🧃";
        }
        if (lower.contains("কোলা") || lower.contains("কোল্ড ড্রিংক") || lower.contains("স্প্রাইট") || lower.contains("পেপসি") || lower.contains("সেভেনআপ")) {
            return "🥤";
        }
        if (lower.contains("চিপ") || lower.contains("চিপস") || lower.contains("কুরকুরে") || lower.contains("ঝালমুড়ি") || lower.contains("মুড়ি")) {
            return "🍿";
        }
        if (lower.contains("সাবান") || lower.contains("সার্ফ") || lower.contains("ডিটারজেন্ট") || lower.contains("শ্যাম্পু") || lower.contains("ফ্রেশ") || lower.contains("ফ্রেস")) {
            return "🧼";
        }
        if (lower.contains("টুথপেস্ট") || lower.contains("ব্রাশ") || lower.contains("পেস্ট")) {
            return "🪥";
        }
        if (lower.contains("সিগারেট") || lower.contains("মেরিস") || lower.contains("বিড়ি") || lower.contains("তামাক") || lower.contains("পান") || lower.contains("সুপারি")) {
            return "🚬";
        }
        if (lower.contains("মশা") || lower.contains("কয়েল") || lower.contains("গুডনাইট")) {
            return "🌀";
        }
        if (lower.contains("ওষুধ") || lower.contains("নাপা") || lower.contains("প্যারাসিটামল") || lower.contains("স্যালাইন")) {
            return "💊";
        }
        if (lower.contains("মাছ") || lower.contains("মাংস") || lower.contains("মুরগি") || lower.contains("গরু")) {
            return "🍗";
        }
        if (lower.contains("ফল") || lower.contains("আম") || lower.contains("কলা") || lower.contains("আপেল") || lower.contains("কমলা")) {
            return "🍌";
        }
        if (lower.contains("শাক") || lower.contains("সবজি") || lower.contains("তরকারি") || lower.contains("বাজার") || lower.contains("সদাই")) {
            return "🛒";
        }
        if (lower.contains("গাড়ি") || lower.contains("ভাড়া") || lower.contains("পরিবহন") || lower.contains("রিকশা") || lower.contains("ভ্যান") || lower.contains("স্টার লাইন") || lower.contains("স্টারলাইন")) {
            return "🚚";
        }
        if (lower.contains("দোকান") || lower.contains("ঘর ভাড়া") || lower.contains("বস্তি")) {
            return "🏠";
        }
        if (lower.contains("বিদ্যুৎ") || lower.contains("কারেন্ট") || lower.contains("বিল") || lower.contains("মিটার") || lower.contains("পাওযার")) {
            return "⚡";
        }
        if (lower.contains("বিকাশ") || lower.contains("নগদ") || lower.contains("রকেট") || lower.contains("উপায়")) {
            return "📱";
        }
        if (lower.contains("ব্যাংক") || lower.contains("চেক") || lower.contains("ড্রাফট")) {
            return "🏦";
        }
        if (lower.contains("কিস্তি") || lower.contains("ঋণ") || lower.contains("সিজন")) {
            return "🗓️";
        }
        if (lower.contains("বেতন") || lower.contains("মুজুরি") || lower.contains("হাজিরা")) {
            return "💼";
        }
        if (lower.contains("পলিথিন") || lower.contains("ব্যাগ") || lower.contains("প্যাকেট") || lower.contains("স্টেশনারি") || lower.contains("স্টাশনারি")) {
            return "🛍️";
        }
        return "📦";
    }

    private void setupAutocomplete() {
        if (this.binding == null) {
            return;
        }
        List<String> rawSuggestions = StorageManager.getInstance(this).getAllProductSuggestionsWithDefaults();
        final List<ExpenseSuggestion> suggestions = new ArrayList<>();
        for (String raw : rawSuggestions) {
            if (raw != null && !raw.trim().isEmpty()) {
                String cleanName = raw.trim();
                suggestions.add(new ExpenseSuggestion(getEmojiForProductName(cleanName), cleanName));
            }
        }
        ArrayAdapter<ExpenseSuggestion> adapter = new ArrayAdapter<ExpenseSuggestion>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(suggestions)) {
            private final List<ExpenseSuggestion> originalList = new ArrayList<>(suggestions);

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ExpenseSuggestion item = getItem(position);
                    if (item != null) {
                        ((TextView) view).setText(item.emoji + "  " + item.name);
                        ((TextView) view).setPadding(24, 20, 24, 20);
                        ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.0f);
                    }
                }
                return view;
            }

            @Override
            public Filter getFilter() {
                return new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        List<ExpenseSuggestion> filtered = new ArrayList<>();
                        if (constraint == null || constraint.toString().trim().isEmpty()) {
                            filtered.addAll(originalList);
                        } else {
                            String query = constraint.toString().trim().toLowerCase();
                            for (ExpenseSuggestion item : originalList) {
                                if (item.name.toLowerCase().contains(query)) {
                                    filtered.add(item);
                                }
                            }
                        }
                        results.values = filtered;
                        results.count = filtered.size();
                        return results;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        clear();
                        if (results != null && results.count > 0 && results.values instanceof List) {
                            addAll((List<ExpenseSuggestion>) results.values);
                        }
                        notifyDataSetChanged();
                    }

                    @Override
                    public CharSequence convertResultToString(Object resultValue) {
                        if (resultValue instanceof ExpenseSuggestion) {
                            return ((ExpenseSuggestion) resultValue).name;
                        }
                        return super.convertResultToString(resultValue);
                    }
                };
            }
        };
        this.binding.etExpenseName.setAdapter(adapter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes5.dex */
    public static class ExpenseSuggestion {
        final String emoji;
        final String name;

        ExpenseSuggestion(String emoji, String name) {
            this.emoji = emoji;
            this.name = name;
        }

        public String toString() {
            return this.name;
        }
    }

    private void setupBakiKhata() {
        if (this.binding == null) {
            return;
        }
        updateBakiKhataUI();
        this.binding.btnSaveBakiRecord.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda54
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m6992lambda$setupBakiKhata$59$comexampleMainActivity(view);
            }
        });
        this.binding.etBakiSearch.addTextChangedListener(new TextWatcher() { // from class: com.example.MainActivity.23
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                MainActivity.this.updateBakiKhataUI();
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupBakiKhata$59$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6992lambda$setupBakiKhata$59$comexampleMainActivity(View v) {
        String name = this.binding.etBakiCustomerName.getText().toString().trim();
        String amountStr = this.binding.etBakiAmount.getText().toString().trim();
        String details = this.binding.etBakiDetails.getText().toString().trim();
        boolean isEmpty = name.isEmpty();
        ActivityMainBinding activityMainBinding = this.binding;
        if (isEmpty) {
            activityMainBinding.tilBakiCustomerName.setError("খরিদ্দারের নাম লিখুন");
            return;
        }
        activityMainBinding.tilBakiCustomerName.setError(null);
        boolean isEmpty2 = amountStr.isEmpty();
        ActivityMainBinding activityMainBinding2 = this.binding;
        if (isEmpty2) {
            activityMainBinding2.tilBakiAmount.setError("বাকির পরিমাণ লিখুন");
            return;
        }
        activityMainBinding2.tilBakiAmount.setError(null);
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0.0d) {
                this.binding.tilBakiAmount.setError("সঠিক বকেয়া সংখ্যা লিখুন");
                return;
            }
            String id = UUID.randomUUID().toString();
            String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            BakiModel record = new BakiModel(id, name, amount, currentDate, details);
            StorageManager storage = StorageManager.getInstance(this);
            List<BakiModel> bakiList = storage.loadBakiRecords();
            bakiList.add(0, record);
            storage.saveBakiRecords(bakiList);
            this.binding.etBakiCustomerName.setText("");
            this.binding.etBakiAmount.setText("");
            this.binding.etBakiDetails.setText("");
            View view = getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            Toast.makeText(this, "✅ বাকি হিসাব সফলভাবে যুক্ত হয়েছে!", 0).show();
            updateBakiKhataUI();
            triggerAutoCloudBackup();
        } catch (Exception e) {
            this.binding.tilBakiAmount.setError("বাকির পরিমাণ সঠিক সংখ্যা হতে হবে");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBakiKhataUI() {
        if (this.binding == null) {
            return;
        }
        StorageManager storage = StorageManager.getInstance(this);
        List<BakiModel> allBaki = storage.loadBakiRecords();
        double totalAmount = 0.0d;
        Iterator<BakiModel> it = allBaki.iterator();
        while (it.hasNext()) {
            totalAmount += it.next().getAmount();
        }
        this.binding.tvTotalBakiAmount.setText(String.format(Locale.getDefault(), "৳ %,.0f", Double.valueOf(totalAmount)));
        this.binding.tvTotalBakiCustomers.setText(allBaki.size() + " জন");
        Set<String> uniqueNames = new HashSet<>();
        for (BakiModel item : allBaki) {
            if (item.getCustomerName() != null && !item.getCustomerName().trim().isEmpty()) {
                uniqueNames.add(item.getCustomerName().trim());
            }
        }
        List<String> suggestions = new ArrayList<>(uniqueNames);
        ArrayAdapter<String> bakiAutocompleteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, suggestions);
        this.binding.etBakiCustomerName.setAdapter(bakiAutocompleteAdapter);
        String query = this.binding.etBakiSearch.getText().toString().trim().toLowerCase();
        List<BakiModel> filteredList = new ArrayList<>();
        for (BakiModel item2 : allBaki) {
            if (item2.getCustomerName().toLowerCase().contains(query)) {
                filteredList.add(item2);
            }
        }
        populateBakiList(filteredList);
    }

    private void populateBakiList(List<BakiModel> list) {
        this.binding.layoutBakiList.removeAllViews();
        boolean isEmpty = list.isEmpty();
        ActivityMainBinding activityMainBinding = this.binding;
        int i = 0;
        if (!isEmpty) {
            activityMainBinding.layoutBakiEmptyState.setVisibility(8);
            this.binding.layoutBakiList.setVisibility(0);
            Iterator<BakiModel> it = list.iterator();
            while (it.hasNext()) {
                final BakiModel item = it.next();
                MaterialCardView materialCardView = new MaterialCardView(this);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
                cardParams.setMargins(i, i, i, dpToPx(12));
                materialCardView.setLayoutParams(cardParams);
                materialCardView.setRadius(dpToPx(16));
                materialCardView.setCardElevation(dpToPx(1));
                materialCardView.setStrokeColor(Color.parseColor("#E2E8F0"));
                materialCardView.setStrokeWidth(dpToPx(1));
                materialCardView.setContentPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
                LinearLayout linearLayout = new LinearLayout(this);
                linearLayout.setOrientation(1);
                LinearLayout linearLayout2 = new LinearLayout(this);
                linearLayout2.setOrientation(i);
                linearLayout2.setGravity(16);
                TextView avatar = new TextView(this);
                LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36));
                avatarParams.setMargins(i, i, dpToPx(10), i);
                avatar.setLayoutParams(avatarParams);
                avatar.setGravity(17);
                avatar.setTextColor(-1);
                avatar.setTextSize(14.0f);
                avatar.setTypeface(null, 1);
                avatar.setBackground(createCircleDrawable(item.getCustomerName()));
                avatar.setText(getInitials(item.getCustomerName()));
                linearLayout2.addView(avatar);
                LinearLayout textContainer = new LinearLayout(this);
                textContainer.setOrientation(1);
                LinearLayout.LayoutParams textContainerParams = new LinearLayout.LayoutParams(i, -2, 1.0f);
                textContainer.setLayoutParams(textContainerParams);
                TextView txtName = new TextView(this);
                txtName.setText(item.getCustomerName());
                txtName.setTextSize(14.0f);
                txtName.setTypeface(null, 1);
                txtName.setTextColor(Color.parseColor("#1E293B"));
                textContainer.addView(txtName);
                if (item.getDetails() != null && !item.getDetails().trim().isEmpty()) {
                    TextView txtDetails = new TextView(this);
                    txtDetails.setText("📝 " + item.getDetails());
                    txtDetails.setTextSize(11.0f);
                    txtDetails.setTextColor(Color.parseColor("#64748B"));
                    txtDetails.setPadding(0, dpToPx(2), 0, 0);
                    textContainer.addView(txtDetails);
                }
                TextView txtDate = new TextView(this);
                txtDate.setText("📅 " + item.getDate());
                txtDate.setTextSize(10.0f);
                txtDate.setTextColor(Color.parseColor("#94A3B8"));
                txtDate.setPadding(0, dpToPx(2), 0, 0);
                textContainer.addView(txtDate);
                linearLayout2.addView(textContainer);
                TextView txtAmount = new TextView(this);
                Iterator<BakiModel> it2 = it;
                txtAmount.setText(String.format(Locale.getDefault(), "৳ %,.0f", Double.valueOf(item.getAmount())));
                txtAmount.setTextSize(15.0f);
                txtAmount.setTypeface(null, 1);
                txtAmount.setTextColor(Color.parseColor("#DC2626"));
                linearLayout2.addView(txtAmount);
                linearLayout.addView(linearLayout2);
                LinearLayout linearLayout3 = new LinearLayout(this);
                LinearLayout.LayoutParams actionRowParams = new LinearLayout.LayoutParams(-1, -2);
                actionRowParams.setMargins(0, dpToPx(10), 0, 0);
                linearLayout3.setLayoutParams(actionRowParams);
                linearLayout3.setOrientation(0);
                linearLayout3.setGravity(GravityCompat.END);
                MaterialButton btnPay = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonStyle);
                btnPay.setText("জমা নিন");
                btnPay.setTextSize(11.0f);
                btnPay.setPadding(dpToPx(10), 0, dpToPx(10), 0);
                LinearLayout.LayoutParams btnPayParams = new LinearLayout.LayoutParams(-2, dpToPx(34));
                btnPayParams.setMargins(0, 0, dpToPx(8), 0);
                btnPay.setLayoutParams(btnPayParams);
                btnPay.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#059669")));
                btnPay.setTextColor(-1);
                btnPay.setCornerRadius(dpToPx(8));
                btnPay.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda60
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        MainActivity.this.m6985lambda$populateBakiList$60$comexampleMainActivity(item, view);
                    }
                });
                linearLayout3.addView(btnPay);
                MaterialButton btnAddDue = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                btnAddDue.setText("+ বাকি");
                btnAddDue.setTextSize(11.0f);
                btnAddDue.setPadding(dpToPx(8), 0, dpToPx(8), 0);
                LinearLayout.LayoutParams btnAddDueParams = new LinearLayout.LayoutParams(-2, dpToPx(34));
                btnAddDueParams.setMargins(0, 0, dpToPx(8), 0);
                btnAddDue.setLayoutParams(btnAddDueParams);
                btnAddDue.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#EA580C")));
                btnAddDue.setStrokeWidth(dpToPx(1));
                btnAddDue.setTextColor(Color.parseColor("#EA580C"));
                btnAddDue.setCornerRadius(dpToPx(8));
                btnAddDue.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda61
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        MainActivity.this.m6986lambda$populateBakiList$61$comexampleMainActivity(item, view);
                    }
                });
                linearLayout3.addView(btnAddDue);
                MaterialButton btnShare = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                btnShare.setText("তাগাদা দিন");
                btnShare.setTextSize(11.0f);
                btnShare.setPadding(dpToPx(8), 0, dpToPx(8), 0);
                LinearLayout.LayoutParams btnShareParams = new LinearLayout.LayoutParams(-2, dpToPx(34));
                btnShareParams.setMargins(0, 0, dpToPx(8), 0);
                btnShare.setLayoutParams(btnShareParams);
                btnShare.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#D97706")));
                btnShare.setStrokeWidth(dpToPx(1));
                btnShare.setTextColor(Color.parseColor("#D97706"));
                btnShare.setCornerRadius(dpToPx(8));
                btnShare.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_share));
                btnShare.setIconSize(dpToPx(12));
                btnShare.setIconGravity(2);
                btnShare.setIconTint(ColorStateList.valueOf(Color.parseColor("#D97706")));
                btnShare.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda62
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        MainActivity.this.m6987lambda$populateBakiList$62$comexampleMainActivity(item, view);
                    }
                });
                linearLayout3.addView(btnShare);
                MaterialButton btnDelete = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                btnDelete.setText("");
                LinearLayout.LayoutParams btnDeleteParams = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(34));
                btnDelete.setLayoutParams(btnDeleteParams);
                btnDelete.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#EF4444")));
                btnDelete.setStrokeWidth(dpToPx(1));
                btnDelete.setTextColor(Color.parseColor("#EF4444"));
                btnDelete.setCornerRadius(dpToPx(8));
                btnDelete.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_trash));
                btnDelete.setIconSize(dpToPx(14));
                btnDelete.setIconPadding(0);
                btnDelete.setIconGravity(2);
                btnDelete.setIconTint(ColorStateList.valueOf(Color.parseColor("#EF4444")));
                btnDelete.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda63
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        MainActivity.this.m6988lambda$populateBakiList$63$comexampleMainActivity(item, view);
                    }
                });
                linearLayout3.addView(btnDelete);
                linearLayout.addView(linearLayout3);
                materialCardView.addView(linearLayout);
                this.binding.layoutBakiList.addView(materialCardView);
                it = it2;
                i = 0;
            }
            return;
        }
        activityMainBinding.layoutBakiEmptyState.setVisibility(0);
        this.binding.layoutBakiList.setVisibility(8);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$populateBakiList$60$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6985lambda$populateBakiList$60$comexampleMainActivity(BakiModel item, View v) {
        showReceivePaymentDialog(item);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$populateBakiList$61$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6986lambda$populateBakiList$61$comexampleMainActivity(BakiModel item, View v) {
        this.binding.etBakiCustomerName.setText(item.getCustomerName());
        this.binding.etBakiCustomerName.setSelection(item.getCustomerName().length());
        this.binding.etBakiAmount.requestFocus();
        this.binding.nestedScrollView.smoothScrollTo(0, this.binding.layoutBakiKhata.getTop());
        Toast.makeText(this, "✍️ " + item.getCustomerName() + " এর জন্য নতুন বাকি হিসাব লিখতে এমাউন্ট দিন।", 0).show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$populateBakiList$62$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6987lambda$populateBakiList$62$comexampleMainActivity(BakiModel item, View v) {
        shareBakiReminder(item);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$populateBakiList$63$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6988lambda$populateBakiList$63$comexampleMainActivity(BakiModel item, View v) {
        deleteBakiRecord(item);
    }

    private void showReceivePaymentDialog(final BakiModel item) {
        TextInputLayout til = new TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        til.setHint("পরিশোধকৃত টাকা (৳)");
        til.setBoxStrokeColor(Color.parseColor("#059669"));
        til.setHintTextColor(ColorStateList.valueOf(Color.parseColor("#059669")));
        final TextInputEditText et = new TextInputEditText(this);
        et.setInputType(8194);
        et.setText(String.format(Locale.US, "%.0f", Double.valueOf(item.getAmount())));
        til.addView(et);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(1);
        int padding = dpToPx(20);
        container.setPadding(padding, padding, padding, padding);
        container.addView(til);
        new MaterialAlertDialogBuilder(this).setTitle((CharSequence) "💵 বাকি টাকা জমা নিন").setMessage((CharSequence) ("গ্রাহক '" + item.getCustomerName() + "' থেকে কত টাকা জমা পেয়েছেন তা লিখুন।")).setView((View) container).setPositiveButton((CharSequence) "জমা করুন", new DialogInterface.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda4
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m7045lambda$showReceivePaymentDialog$64$comexampleMainActivity(et, item, dialogInterface, i);
            }
        }).setNegativeButton((CharSequence) "বাতিল", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showReceivePaymentDialog$64$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7045lambda$showReceivePaymentDialog$64$comexampleMainActivity(TextInputEditText et, BakiModel item, DialogInterface dialog, int which) {
        String valStr = et.getText().toString().trim();
        if (valStr.isEmpty()) {
            return;
        }
        try {
            double val = Double.parseDouble(valStr);
            if (val <= 0.0d) {
                Toast.makeText(this, "⚠️ ভুল এমাউন্ট প্রবেশ করানো হয়েছে।", 0).show();
                return;
            }
            StorageManager storage = StorageManager.getInstance(this);
            List<BakiModel> bakiList = storage.loadBakiRecords();
            int targetIndex = -1;
            int i = 0;
            while (true) {
                if (i >= bakiList.size()) {
                    break;
                }
                if (!bakiList.get(i).getId().equals(item.getId())) {
                    i++;
                } else {
                    targetIndex = i;
                    break;
                }
            }
            if (targetIndex != -1) {
                BakiModel b = bakiList.get(targetIndex);
                double newAmt = b.getAmount() - val;
                if (newAmt <= 0.0d) {
                    bakiList.remove(targetIndex);
                    Toast.makeText(this, "🎉 গ্রাহকের সমস্ত বাকি পরিশোধ হয়েছে!", 1).show();
                } else {
                    b.setAmount(newAmt);
                    String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                    b.setDate(currentDate);
                    Toast.makeText(this, "✅ " + val + " টাকা জমা নেওয়া হয়েছে!", 0).show();
                }
                storage.saveBakiRecords(bakiList);
                updateBakiKhataUI();
                triggerAutoCloudBackup();
            }
        } catch (Exception e) {
            Toast.makeText(this, "⚠️ সঠিক সংখ্যা লিখুন।", 0).show();
        }
    }

    private void shareBakiReminder(BakiModel item) {
        String msg = "জনাব " + item.getCustomerName() + ",\nআপনার নিকট আমাদের মোট বকেয়া পাওনার পরিমাণ: ৳ " + String.format(Locale.getDefault(), "%,.0f", Double.valueOf(item.getAmount())) + " টাকা।\nঅনুগ্রহ করে বকেয়া টাকা পরিশোধ করে সাহায্য করুন।\n\nধন্যবাদান্তে,\nআমার ক্যাশ খাতা";
        ClipboardManager clipboard = (ClipboardManager) getSystemService("clipboard");
        ClipData clip = ClipData.newPlainText("Baki Reminder", msg);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "✅ তাগাদা মেসেজ কপি করা হয়েছে!", 0).show();
        Intent sendIntent = new Intent();
        sendIntent.setAction("android.intent.action.SEND");
        sendIntent.putExtra("android.intent.extra.TEXT", msg);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "তাগাদা মেসেজ পাঠান"));
    }

    private void deleteBakiRecord(final BakiModel item) {
        new MaterialAlertDialogBuilder(this).setTitle((CharSequence) "⚠️ হিসাব মুছে ফেলবেন?").setMessage((CharSequence) ("আপনি কি নিশ্চিতভাবে '" + item.getCustomerName() + "' এর এই বাকি হিসাবটি মুছে ফেলতে চান? এটি আর ফিরিয়ে আনা যাবে না।")).setPositiveButton((CharSequence) "হ্যাঁ, মুছুন", new DialogInterface.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda38
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m6972lambda$deleteBakiRecord$65$comexampleMainActivity(item, dialogInterface, i);
            }
        }).setNegativeButton((CharSequence) "বাতিল", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$deleteBakiRecord$65$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6972lambda$deleteBakiRecord$65$comexampleMainActivity(BakiModel item, DialogInterface dialog, int which) {
        StorageManager storage = StorageManager.getInstance(this);
        List<BakiModel> bakiList = storage.loadBakiRecords();
        int targetIndex = -1;
        int i = 0;
        while (true) {
            if (i >= bakiList.size()) {
                break;
            }
            if (!bakiList.get(i).getId().equals(item.getId())) {
                i++;
            } else {
                targetIndex = i;
                break;
            }
        }
        if (targetIndex != -1) {
            bakiList.remove(targetIndex);
            storage.saveBakiRecords(bakiList);
            Toast.makeText(this, "🗑️ হিসাবটি মুছে ফেলা হয়েছে!", 0).show();
            updateBakiKhataUI();
            triggerAutoCloudBackup();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable createCircleDrawable(String name) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(1);
        int hash = name != null ? name.hashCode() : 0;
        int index = Math.abs(hash) % 5;
        String[] colors = {"#EA580C", "#2563EB", "#059669", "#7C3AED", "#DB2777"};
        shape.setColor(Color.parseColor(colors[index]));
        return shape;
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "B";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length > 0) {
            String first = parts[0];
            if (first.length() > 0) {
                return first.substring(0, 1).toUpperCase();
            }
        }
        return "B";
    }

    private void setupFordiKhata() {
        if (this.binding == null) {
            return;
        }
        this.binding.etFordiSearch.addTextChangedListener(new TextWatcher() { // from class: com.example.MainActivity.24
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                MainActivity.this.updateFordiKhataUI();
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
            }
        });
        this.binding.btnCreateFordi.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda14
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m7009lambda$setupFordiKhata$66$comexampleMainActivity(view);
            }
        });
        updateFordiKhataUI();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupFordiKhata$66$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7009lambda$setupFordiKhata$66$comexampleMainActivity(View v) {
        String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String banglaDate = new SimpleDateFormat("dd MMMM", new Locale("bn", "BD")).format(new Date());
        String id = UUID.randomUUID().toString();
        FordiModel newFordi = new FordiModel(id, banglaDate, dateStr, new ArrayList(), "#F0FDFA");
        StorageManager storage = StorageManager.getInstance(this);
        List<FordiModel> allFordi = storage.loadFordiRecords();
        allFordi.add(0, newFordi);
        storage.saveFordiRecords(allFordi);
        updateFordiKhataUI();
        triggerAutoCloudBackup();
        showFordiDetailDialog(newFordi);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFordiKhataUI() {
        if (this.binding == null) {
            return;
        }
        StorageManager storage = StorageManager.getInstance(this);
        List<FordiModel> allFordi = storage.loadFordiRecords();
        double totalBudget = 0.0d;
        Iterator<FordiModel> it = allFordi.iterator();
        while (it.hasNext()) {
            for (FordiItemModel item : it.next().getItems()) {
                totalBudget += item.getPrice();
            }
        }
        this.binding.tvTotalFordiCount.setText(allFordi.size() + " টি");
        this.binding.tvTotalFordiBudget.setText(String.format(Locale.getDefault(), "৳ %,.0f", Double.valueOf(totalBudget)));
        String query = this.binding.etFordiSearch.getText().toString().trim().toLowerCase();
        List<FordiModel> filteredList = new ArrayList<>();
        for (FordiModel fordi : allFordi) {
            boolean matchesTitle = fordi.getTitle().toLowerCase().contains(query);
            boolean matchesItems = false;
            Iterator<FordiItemModel> it2 = fordi.getItems().iterator();
            while (true) {
                if (!it2.hasNext()) {
                    break;
                }
                FordiItemModel item2 = it2.next();
                if (item2.getName().toLowerCase().contains(query)) {
                    matchesItems = true;
                    break;
                }
            }
            if (matchesTitle || matchesItems) {
                filteredList.add(fordi);
            }
        }
        populateFordiGrid(filteredList);
    }

    private void populateFordiGrid(List<FordiModel> list) {
        this.binding.layoutFordiList.removeAllViews();
        boolean isEmpty = list.isEmpty();
        ActivityMainBinding activityMainBinding = this.binding;
        int i = 0;
        if (!isEmpty) {
            activityMainBinding.layoutFordiEmptyState.setVisibility(8);
            this.binding.layoutFordiList.setVisibility(0);
            Iterator<FordiModel> it = list.iterator();
            while (it.hasNext()) {
                final FordiModel fordi = it.next();
                MaterialCardView materialCardView = new MaterialCardView(this);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
                cardParams.setMargins(i, i, i, dpToPx(16));
                materialCardView.setLayoutParams(cardParams);
                materialCardView.setRadius(dpToPx(20));
                materialCardView.setCardElevation(dpToPx(1));
                materialCardView.setStrokeWidth(dpToPx(1));
                materialCardView.setStrokeColor(Color.parseColor("#E2E8F0"));
                String bgHex = fordi.getColorHex() != null ? fordi.getColorHex() : "#F1F5F9";
                materialCardView.setCardBackgroundColor(Color.parseColor(bgHex));
                materialCardView.setContentPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
                LinearLayout linearLayout = new LinearLayout(this);
                linearLayout.setOrientation(1);
                LinearLayout headerRow = new LinearLayout(this);
                headerRow.setOrientation(i);
                headerRow.setGravity(16);
                LinearLayout titleLayout = new LinearLayout(this);
                titleLayout.setOrientation(1);
                LinearLayout.LayoutParams titleLayoutParams = new LinearLayout.LayoutParams(i, -2, 1.0f);
                titleLayout.setLayoutParams(titleLayoutParams);
                TextView txtTitle = new TextView(this);
                txtTitle.setText(fordi.getTitle());
                txtTitle.setTextSize(14.0f);
                txtTitle.setTypeface(null, 1);
                txtTitle.setTextColor(Color.parseColor("#0F172A"));
                titleLayout.addView(txtTitle);
                TextView txtDate = new TextView(this);
                txtDate.setText("📅 " + fordi.getDate());
                txtDate.setTextSize(10.0f);
                txtDate.setTextColor(Color.parseColor("#475569"));
                txtDate.setPadding(i, dpToPx(2), i, i);
                titleLayout.addView(txtDate);
                headerRow.addView(titleLayout);
                int totalItems = fordi.getItems().size();
                double listTotal = 0.0d;
                int checkedItems = 0;
                for (FordiItemModel item : fordi.getItems()) {
                    listTotal += item.getPrice();
                    if (item.isChecked()) {
                        checkedItems++;
                    }
                }
                TextView txtProgress = new TextView(this);
                Iterator<FordiModel> it2 = it;
                txtProgress.setText("টিককৃত: " + checkedItems + "/" + totalItems);
                txtProgress.setTextSize(11.0f);
                txtProgress.setTextColor(Color.parseColor("#0F766E"));
                txtProgress.setTypeface(null, 1);
                int dpToPx = dpToPx(8);
                int dpToPx2 = dpToPx(4);
                int checkedItems2 = dpToPx(8);
                txtProgress.setPadding(dpToPx, dpToPx2, checkedItems2, dpToPx(4));
                GradientDrawable pill = new GradientDrawable();
                pill.setShape(0);
                pill.setCornerRadius(dpToPx(10));
                pill.setColor(Color.parseColor("#CCFBF1"));
                txtProgress.setBackground(pill);
                headerRow.addView(txtProgress);
                linearLayout.addView(headerRow);
                View divider = new View(this);
                LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(-1, dpToPx(1));
                divParams.setMargins(0, dpToPx(10), 0, dpToPx(10));
                divider.setLayoutParams(divParams);
                divider.setBackgroundColor(Color.parseColor("#E2E8F0"));
                linearLayout.addView(divider);
                int previewCount = Math.min(totalItems, 5);
                if (previewCount == 0) {
                    TextView emptyPreview = new TextView(this);
                    emptyPreview.setText("ফর্দটি শূন্য। পণ্য যোগ করতে নিচে খুলুন চাপুন।");
                    emptyPreview.setTextSize(12.0f);
                    emptyPreview.setTextColor(Color.parseColor("#64748B"));
                    emptyPreview.setTypeface(null, 2);
                    linearLayout.addView(emptyPreview);
                } else {
                    int i2 = 0;
                    while (i2 < previewCount) {
                        FordiItemModel item2 = fordi.getItems().get(i2);
                        int i3 = i2;
                        LinearLayout row = new LinearLayout(this);
                        row.setOrientation(0);
                        row.setGravity(16);
                        int previewCount2 = previewCount;
                        LinearLayout headerRow2 = headerRow;
                        row.setPadding(0, dpToPx(2), 0, dpToPx(2));
                        TextView bullet = new TextView(this);
                        bullet.setText(item2.isChecked() ? "☑️ " : "⬜ ");
                        bullet.setTextSize(12.0f);
                        row.addView(bullet);
                        TextView nameView = new TextView(this);
                        nameView.setText(item2.getName());
                        nameView.setTextSize(13.0f);
                        View divider2 = divider;
                        LinearLayout.LayoutParams divParams2 = divParams;
                        nameView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                        if (item2.isChecked()) {
                            nameView.setPaintFlags(nameView.getPaintFlags() | 16);
                            nameView.setTextColor(Color.parseColor("#94A3B8"));
                        } else {
                            nameView.setTextColor(Color.parseColor("#334155"));
                        }
                        row.addView(nameView);
                        if (item2.getPrice() > 0.0d) {
                            TextView priceView = new TextView(this);
                            priceView.setText("৳" + String.format(Locale.getDefault(), "%,.0f", Double.valueOf(item2.getPrice())));
                            priceView.setTextSize(12.0f);
                            priceView.setTypeface(null, 1);
                            priceView.setTextColor(Color.parseColor("#0D9488"));
                            row.addView(priceView);
                        }
                        linearLayout.addView(row);
                        i2 = i3 + 1;
                        divider = divider2;
                        previewCount = previewCount2;
                        headerRow = headerRow2;
                        divParams = divParams2;
                    }
                    if (totalItems > 5) {
                        TextView moreView = new TextView(this);
                        moreView.setText("+ আরো " + (totalItems - 5) + " টি পণ্য...");
                        moreView.setTextSize(11.0f);
                        moreView.setTextColor(Color.parseColor("#64748B"));
                        moreView.setPadding(0, dpToPx(4), 0, 0);
                        linearLayout.addView(moreView);
                    }
                }
                TextView txtTotal = new TextView(this);
                txtTotal.setText("সম্ভাব্য মোট খরচ: ৳" + String.format(Locale.getDefault(), "%,.0f", Double.valueOf(listTotal)));
                txtTotal.setTextSize(12.0f);
                txtTotal.setTypeface(null, 1);
                txtTotal.setTextColor(Color.parseColor("#0D9488"));
                txtTotal.setPadding(0, dpToPx(10), 0, dpToPx(4));
                linearLayout.addView(txtTotal);
                LinearLayout linearLayout2 = new LinearLayout(this);
                linearLayout2.setOrientation(0);
                linearLayout2.setGravity(GravityCompat.END);
                LinearLayout.LayoutParams btnRowParams = new LinearLayout.LayoutParams(-1, -2);
                btnRowParams.setMargins(0, dpToPx(10), 0, 0);
                linearLayout2.setLayoutParams(btnRowParams);
                MaterialButton btnEdit = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonStyle);
                btnEdit.setText("ফর্দ খুলুন / এডিট");
                btnEdit.setTextSize(11.0f);
                btnEdit.setPadding(dpToPx(12), 0, dpToPx(12), 0);
                LinearLayout.LayoutParams btnEditParams = new LinearLayout.LayoutParams(-2, dpToPx(34));
                btnEditParams.setMargins(0, 0, dpToPx(8), 0);
                btnEdit.setLayoutParams(btnEditParams);
                btnEdit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0D9488")));
                btnEdit.setCornerRadius(dpToPx(8));
                btnEdit.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda6
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        MainActivity.this.m6989lambda$populateFordiGrid$67$comexampleMainActivity(fordi, view);
                    }
                });
                linearLayout2.addView(btnEdit);
                MaterialButton btnDelete = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                btnDelete.setText("");
                LinearLayout.LayoutParams btnDeleteParams = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(34));
                btnDelete.setLayoutParams(btnDeleteParams);
                btnDelete.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#EF4444")));
                btnDelete.setStrokeWidth(dpToPx(1));
                btnDelete.setCornerRadius(dpToPx(8));
                btnDelete.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_trash));
                btnDelete.setIconSize(dpToPx(14));
                btnDelete.setIconPadding(0);
                btnDelete.setIconGravity(2);
                btnDelete.setIconTint(ColorStateList.valueOf(Color.parseColor("#EF4444")));
                btnDelete.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda7
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        MainActivity.this.m6990lambda$populateFordiGrid$68$comexampleMainActivity(fordi, view);
                    }
                });
                linearLayout2.addView(btnDelete);
                linearLayout.addView(linearLayout2);
                materialCardView.addView(linearLayout);
                this.binding.layoutFordiList.addView(materialCardView);
                it = it2;
                i = 0;
            }
            return;
        }
        activityMainBinding.layoutFordiEmptyState.setVisibility(0);
        this.binding.layoutFordiList.setVisibility(8);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$populateFordiGrid$67$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6989lambda$populateFordiGrid$67$comexampleMainActivity(FordiModel fordi, View v) {
        showFordiDetailDialog(fordi);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$populateFordiGrid$68$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6990lambda$populateFordiGrid$68$comexampleMainActivity(FordiModel fordi, View v) {
        deleteFordiRecord(fordi);
    }

    private void deleteFordiRecord(final FordiModel fordi) {
        new MaterialAlertDialogBuilder(this).setTitle((CharSequence) "🗑️ ফর্দ মুছে ফেলতে চান?").setMessage((CharSequence) ("আপনি কি নিশ্চিতভাবে '" + fordi.getTitle() + "' ফর্দটি মুছে ফেলতে চান? এটি আর ফিরিয়ে আনা যাবে না।")).setPositiveButton((CharSequence) "হ্যাঁ, মুছুন", new DialogInterface.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda56
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m6973lambda$deleteFordiRecord$69$comexampleMainActivity(fordi, dialogInterface, i);
            }
        }).setNegativeButton((CharSequence) "বাতিল", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$deleteFordiRecord$69$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6973lambda$deleteFordiRecord$69$comexampleMainActivity(FordiModel fordi, DialogInterface dialog, int which) {
        StorageManager storage = StorageManager.getInstance(this);
        List<FordiModel> allFordi = storage.loadFordiRecords();
        int targetIndex = -1;
        int i = 0;
        while (true) {
            if (i >= allFordi.size()) {
                break;
            }
            if (!allFordi.get(i).getId().equals(fordi.getId())) {
                i++;
            } else {
                targetIndex = i;
                break;
            }
        }
        if (targetIndex != -1) {
            allFordi.remove(targetIndex);
            storage.saveFordiRecords(allFordi);
            Toast.makeText(this, "🗑️ ফর্দটি মুছে ফেলা হয়েছে!", 0).show();
            updateFordiKhataUI();
            triggerAutoCloudBackup();
        }
    }

    private void showFordiDetailDialog(final FordiModel fordi) {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setFitsSystemWindows(true);
        String colorHex = fordi.getColorHex() != null ? fordi.getColorHex() : "#F1F5F9";
        linearLayout.setBackgroundColor(Color.parseColor(colorHex));
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        linearLayout2.setBackgroundColor(Color.parseColor("#FFFFFF"));
        MaterialButton btnBack = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnBack.setStrokeWidth(0);
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
        btnBack.setLayoutParams(backParams);
        btnBack.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_left_arrow));
        btnBack.setIconSize(dpToPx(18));
        btnBack.setIconPadding(0);
        btnBack.setIconGravity(2);
        btnBack.setIconTint(ColorStateList.valueOf(Color.parseColor("#0F172A")));
        btnBack.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m7039lambda$showFordiDetailDialog$70$comexampleMainActivity(dialog, view);
            }
        });
        linearLayout2.addView(btnBack);
        TextInputLayout tilTitle = new TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        tilTitle.setHint("ফর্দ শিরোনাম (যেমন: ২৫ মে)");
        tilTitle.setBoxStrokeColor(Color.parseColor("#0D9488"));
        tilTitle.setHintTextColor(ColorStateList.valueOf(Color.parseColor("#0D9488")));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        titleParams.setMargins(dpToPx(8), 0, dpToPx(8), 0);
        tilTitle.setLayoutParams(titleParams);
        TextInputEditText etTitleInput = new TextInputEditText(this);
        etTitleInput.setText(fordi.getTitle());
        etTitleInput.setTextSize(13.0f);
        etTitleInput.setInputType(1);
        tilTitle.addView(etTitleInput);
        linearLayout2.addView(tilTitle);
        MaterialButton btnBell = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnBell.setStrokeWidth(0);
        btnBell.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(38), dpToPx(38)));
        btnBell.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_bell));
        btnBell.setIconSize(dpToPx(18));
        btnBell.setIconPadding(0);
        btnBell.setIconGravity(2);
        btnBell.setIconTint(ColorStateList.valueOf(Color.parseColor("#475569")));
        btnBell.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda11
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m7040lambda$showFordiDetailDialog$71$comexampleMainActivity(view);
            }
        });
        linearLayout2.addView(btnBell);
        MaterialButton btnShare = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnShare.setStrokeWidth(0);
        btnShare.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(38), dpToPx(38)));
        btnShare.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_share));
        btnShare.setIconSize(dpToPx(18));
        btnShare.setIconPadding(0);
        btnShare.setIconGravity(2);
        btnShare.setIconTint(ColorStateList.valueOf(Color.parseColor("#475569")));
        btnShare.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda22
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m7041lambda$showFordiDetailDialog$72$comexampleMainActivity(fordi, view);
            }
        });
        linearLayout2.addView(btnShare);
        linearLayout.addView(linearLayout2);
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout3.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        MaterialCardView materialCardView = new MaterialCardView(this);
        materialCardView.setRadius(dpToPx(16));
        materialCardView.setCardElevation(dpToPx(2));
        materialCardView.setStrokeColor(Color.parseColor("#0D9488"));
        materialCardView.setStrokeWidth(dpToPx(1));
        materialCardView.setCardBackgroundColor(-1);
        LinearLayout linearLayout4 = new LinearLayout(this);
        linearLayout4.setOrientation(1);
        linearLayout4.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        LinearLayout linearLayout5 = new LinearLayout(this);
        linearLayout5.setOrientation(0);
        final TextInputLayout tilItemName = new TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        tilItemName.setHint("পণ্যের নাম (যেমন: ডিম)");
        tilItemName.setBoxStrokeColor(Color.parseColor("#0D9488"));
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(0, -2, 1.4f);
        tilItemName.setLayoutParams(itemParams);
        final TextInputEditText etItemName = new TextInputEditText(this);
        etItemName.setTextSize(13.0f);
        tilItemName.addView(etItemName);
        linearLayout5.addView(tilItemName);
        final TextInputLayout tilItemPrice = new TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        tilItemPrice.setHint("৳ দাম (ঐচ্ছিক)");
        tilItemPrice.setBoxStrokeColor(Color.parseColor("#0D9488"));
        LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(0, -2, 0.8f);
        priceParams.setMargins(dpToPx(8), 0, 0, 0);
        tilItemPrice.setLayoutParams(priceParams);
        final TextInputEditText etItemPrice = new TextInputEditText(this);
        etItemPrice.setTextSize(13.0f);
        etItemPrice.setInputType(8194);
        tilItemPrice.addView(etItemPrice);
        linearLayout5.addView(tilItemPrice);
        linearLayout4.addView(linearLayout5);
        MaterialButton btnAddItem = new MaterialButton(this);
        btnAddItem.setText("➕ আরেকটি পণ্য যুক্ত করুন");
        btnAddItem.setTextSize(12.0f);
        btnAddItem.setTextColor(-1);
        btnAddItem.setCornerRadius(dpToPx(12));
        btnAddItem.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0D9488")));
        LinearLayout.LayoutParams addBtnParams = new LinearLayout.LayoutParams(-1, dpToPx(48));
        addBtnParams.setMargins(0, dpToPx(12), 0, 0);
        btnAddItem.setLayoutParams(addBtnParams);
        linearLayout4.addView(btnAddItem);
        materialCardView.addView(linearLayout4);
        linearLayout3.addView(materialCardView);
        linearLayout.addView(linearLayout3);
        NestedScrollView scroll = new NestedScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1.0f);
        scroll.setLayoutParams(scrollParams);
        scroll.setFillViewport(true);
        LinearLayout itemsListContainer = new LinearLayout(this);
        itemsListContainer.setOrientation(1);
        itemsListContainer.setPadding(dpToPx(16), 0, dpToPx(16), dpToPx(16));
        scroll.addView(itemsListContainer);
        linearLayout.addView(scroll);
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(0);
        bottomBar.setGravity(16);
        bottomBar.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        bottomBar.setBackgroundColor(Color.parseColor("#FFFFFF"));
        MaterialButton btnPalette = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnPalette.setStrokeWidth(dpToPx(2));
        btnPalette.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#0D9488")));
        btnPalette.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(44), dpToPx(44)));
        btnPalette.setCornerRadius(dpToPx(22));
        btnPalette.setText("🎨");
        btnPalette.setTextSize(14.0f);
        btnPalette.setPadding(0, 0, 0, 0);
        bottomBar.addView(btnPalette);
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1.0f));
        bottomBar.addView(spacer);
        final TextView tvDialogTotal = new TextView(this);
        tvDialogTotal.setTextSize(15.0f);
        tvDialogTotal.setTypeface(null, 1);
        tvDialogTotal.setTextColor(Color.parseColor("#0F172A"));
        bottomBar.addView(tvDialogTotal);
        linearLayout.addView(bottomBar);
        final Runnable[] refreshDialogTotals = {new Runnable() { // from class: com.example.MainActivity.25
            @Override // java.lang.Runnable
            public void run() {
                double total = 0.0d;
                for (FordiItemModel im : fordi.getItems()) {
                    total += im.getPrice();
                }
                tvDialogTotal.setText("মোট ৳ " + String.format(Locale.getDefault(), "%,.2f", Double.valueOf(total)));
            }
        }};
        final Runnable[] saveCurrentState = {new Runnable() { // from class: com.example.MainActivity.26
            @Override // java.lang.Runnable
            public void run() {
                StorageManager storage = StorageManager.getInstance(MainActivity.this);
                List<FordiModel> allFordi = storage.loadFordiRecords();
                int i = 0;
                while (true) {
                    if (i >= allFordi.size()) {
                        break;
                    }
                    if (!allFordi.get(i).getId().equals(fordi.getId())) {
                        i++;
                    } else {
                        allFordi.set(i, fordi);
                        break;
                    }
                }
                storage.saveFordiRecords(allFordi);
                refreshDialogTotals[0].run();
                MainActivity.this.triggerAutoCloudBackup();
            }
        }};
        final Runnable[] populateItemsList = new Runnable[1];
        populateItemsList[0] = new AnonymousClass27(itemsListContainer, fordi, saveCurrentState, populateItemsList);
        etTitleInput.addTextChangedListener(new TextWatcher() { // from class: com.example.MainActivity.28
            @Override // android.text.TextWatcher
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String val = s.toString().trim();
                if (!val.isEmpty()) {
                    fordi.setTitle(val);
                    saveCurrentState[0].run();
                }
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(Editable s) {
            }
        });
        btnAddItem.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda33
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m7042lambda$showFordiDetailDialog$73$comexampleMainActivity(etItemName, etItemPrice, tilItemName, tilItemPrice, fordi, saveCurrentState, populateItemsList, view);
            }
        });
        btnPalette.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda44
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MainActivity.this.m7044lambda$showFordiDetailDialog$75$comexampleMainActivity(fordi, saveCurrentState, linearLayout, view);
            }
        });
        refreshDialogTotals[0].run();
        populateItemsList[0].run();
        dialog.setContentView(linearLayout);
        dialog.show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showFordiDetailDialog$70$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7039lambda$showFordiDetailDialog$70$comexampleMainActivity(Dialog dialog, View v) {
        dialog.dismiss();
        updateFordiKhataUI();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showFordiDetailDialog$71$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7040lambda$showFordiDetailDialog$71$comexampleMainActivity(View v) {
        Toast.makeText(this, "🔔 নোটিফিকেশন এলার্ট সেট করা হয়েছে!", 0).show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showFordiDetailDialog$72$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7041lambda$showFordiDetailDialog$72$comexampleMainActivity(FordiModel fordi, View v) {
        shareFordiList(fordi);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.example.MainActivity$27, reason: invalid class name */
    /* loaded from: classes5.dex */
    public class AnonymousClass27 implements Runnable {
        final /* synthetic */ FordiModel val$fordi;
        final /* synthetic */ LinearLayout val$itemsListContainer;
        final /* synthetic */ Runnable[] val$populateItemsList;
        final /* synthetic */ Runnable[] val$saveCurrentState;

        AnonymousClass27(final LinearLayout val$itemsListContainer, final FordiModel val$fordi, final Runnable[] val$saveCurrentState, final Runnable[] val$populateItemsList) {
            this.val$itemsListContainer = val$itemsListContainer;
            this.val$fordi = val$fordi;
            this.val$saveCurrentState = val$saveCurrentState;
            this.val$populateItemsList = val$populateItemsList;
        }

        @Override // java.lang.Runnable
        public void run() {
            this.val$itemsListContainer.removeAllViews();
            int i = 0;
            if (this.val$fordi.getItems().isEmpty()) {
                TextView emptyText = new TextView(MainActivity.this);
                emptyText.setText("তালিকায় কোন পণ্য নেই। পণ্য যোগ করুন!");
                emptyText.setGravity(17);
                emptyText.setPadding(0, MainActivity.this.dpToPx(40), 0, MainActivity.this.dpToPx(40));
                emptyText.setTextColor(Color.parseColor("#64748B"));
                emptyText.setTypeface(null, 2);
                this.val$itemsListContainer.addView(emptyText);
                return;
            }
            for (final FordiItemModel item : this.val$fordi.getItems()) {
                MaterialCardView materialCardView = new MaterialCardView(MainActivity.this);
                LinearLayout.LayoutParams icParams = new LinearLayout.LayoutParams(-1, -2);
                icParams.setMargins(i, i, i, MainActivity.this.dpToPx(8));
                materialCardView.setLayoutParams(icParams);
                materialCardView.setRadius(MainActivity.this.dpToPx(12));
                materialCardView.setCardElevation(MainActivity.this.dpToPx(1));
                materialCardView.setStrokeWidth(MainActivity.this.dpToPx(1));
                materialCardView.setStrokeColor(Color.parseColor("#CBD5E1"));
                materialCardView.setContentPadding(MainActivity.this.dpToPx(10), MainActivity.this.dpToPx(8), MainActivity.this.dpToPx(10), MainActivity.this.dpToPx(8));
                LinearLayout linearLayout = new LinearLayout(MainActivity.this);
                linearLayout.setOrientation(i);
                linearLayout.setGravity(16);
                CheckBox cb = new CheckBox(MainActivity.this);
                cb.setChecked(item.isChecked());
                cb.setButtonTintList(ColorStateList.valueOf(Color.parseColor("#0D9488")));
                linearLayout.addView(cb);
                final TextView tvName = new TextView(MainActivity.this);
                tvName.setText(item.getName());
                tvName.setTextSize(14.0f);
                LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(i, -2, 1.0f);
                nameLp.setMargins(MainActivity.this.dpToPx(6), i, MainActivity.this.dpToPx(6), i);
                tvName.setLayoutParams(nameLp);
                if (item.isChecked()) {
                    tvName.setPaintFlags(tvName.getPaintFlags() | 16);
                    tvName.setTextColor(Color.parseColor("#94A3B8"));
                } else {
                    tvName.setTextColor(Color.parseColor("#1E293B"));
                }
                linearLayout.addView(tvName);
                TextView tvPrice = new TextView(MainActivity.this);
                double pr = item.getPrice();
                tvPrice.setText(pr > 0.0d ? "৳ " + String.format(Locale.getDefault(), "%,.0f", Double.valueOf(pr)) : "৳ দাম দিন");
                tvPrice.setTextSize(13.0f);
                tvPrice.setTypeface(null, 1);
                tvPrice.setTextColor(pr > 0.0d ? Color.parseColor("#0D9488") : Color.parseColor("#94A3B8"));
                tvPrice.setPadding(MainActivity.this.dpToPx(8), MainActivity.this.dpToPx(4), MainActivity.this.dpToPx(8), MainActivity.this.dpToPx(4));
                GradientDrawable tagBg = new GradientDrawable();
                tagBg.setShape(0);
                tagBg.setCornerRadius(MainActivity.this.dpToPx(6));
                tagBg.setColor(Color.parseColor("#F1F5F9"));
                tvPrice.setBackground(tagBg);
                linearLayout.addView(tvPrice);
                MaterialButton btnRowDel = new MaterialButton(MainActivity.this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                btnRowDel.setStrokeWidth(0);
                LinearLayout.LayoutParams rdLp = new LinearLayout.LayoutParams(MainActivity.this.dpToPx(34), MainActivity.this.dpToPx(34));
                rdLp.setMargins(MainActivity.this.dpToPx(4), 0, 0, 0);
                btnRowDel.setLayoutParams(rdLp);
                btnRowDel.setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_trash));
                btnRowDel.setIconSize(MainActivity.this.dpToPx(13));
                btnRowDel.setIconPadding(0);
                btnRowDel.setIconGravity(2);
                btnRowDel.setIconTint(ColorStateList.valueOf(Color.parseColor("#EF4444")));
                linearLayout.addView(btnRowDel);
                final Runnable[] runnableArr = this.val$saveCurrentState;
                cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        item.setChecked(isChecked);
                        if (isChecked) {
                            tvName.setPaintFlags(tvName.getPaintFlags() | 16);
                            tvName.setTextColor(Color.parseColor("#94A3B8"));
                        } else {
                            tvName.setPaintFlags(tvName.getPaintFlags() & (~16));
                            tvName.setTextColor(Color.parseColor("#1E293B"));
                        }
                        if (runnableArr[0] != null) {
                            runnableArr[0].run();
                        }
                    }
                });
                final Runnable[] runnableArr2 = this.val$saveCurrentState;
                final LinearLayout linearLayout2 = this.val$itemsListContainer;
                final Runnable[] runnableArr3 = this.val$populateItemsList;
                tvName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        com.google.android.material.textfield.TextInputLayout til = new com.google.android.material.textfield.TextInputLayout(MainActivity.this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
                        til.setHint("পণ্যের নাম");
                        final com.google.android.material.textfield.TextInputEditText et = new com.google.android.material.textfield.TextInputEditText(MainActivity.this);
                        et.setText(item.getName());
                        til.addView(et);
                        LinearLayout dlgLp = new LinearLayout(MainActivity.this);
                        dlgLp.setPadding(MainActivity.this.dpToPx(20), MainActivity.this.dpToPx(16), MainActivity.this.dpToPx(20), MainActivity.this.dpToPx(16));
                        dlgLp.setOrientation(1);
                        dlgLp.addView(til);
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(MainActivity.this)
                            .setTitle("✏️ পণ্য পরিবর্তন")
                            .setView(dlgLp)
                            .setPositiveButton("পরিবর্তন", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String nVal = et.getText().toString().trim();
                                    if (!nVal.isEmpty()) {
                                        item.setName(nVal);
                                        if (runnableArr2[0] != null) {
                                            runnableArr2[0].run();
                                        }
                                        linearLayout2.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (runnableArr3[0] != null) {
                                                    runnableArr3[0].run();
                                                }
                                            }
                                        });
                                    }
                                }
                            })
                            .setNegativeButton("বাতিল", null)
                            .show();
                    }
                });
                final Runnable[] runnableArr4 = this.val$saveCurrentState;
                final LinearLayout linearLayout3 = this.val$itemsListContainer;
                final Runnable[] runnableArr5 = this.val$populateItemsList;
                tvPrice.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        com.google.android.material.textfield.TextInputLayout til = new com.google.android.material.textfield.TextInputLayout(MainActivity.this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
                        til.setHint("৳ পণ্যের মূল্য বা দাম");
                        final com.google.android.material.textfield.TextInputEditText et = new com.google.android.material.textfield.TextInputEditText(MainActivity.this);
                        et.setInputType(8194);
                        et.setText(item.getPrice() > 0.0d ? String.format(Locale.US, "%.0f", Double.valueOf(item.getPrice())) : "");
                        til.addView(et);
                        LinearLayout dlgLp = new LinearLayout(MainActivity.this);
                        dlgLp.setPadding(MainActivity.this.dpToPx(20), MainActivity.this.dpToPx(16), MainActivity.this.dpToPx(20), MainActivity.this.dpToPx(16));
                        dlgLp.setOrientation(1);
                        dlgLp.addView(til);
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(MainActivity.this)
                            .setTitle("✏️ দাম নির্ধারণ")
                            .setView(dlgLp)
                            .setPositiveButton("সংরক্ষণ", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String pVal = et.getText().toString().trim();
                                    double pDouble = 0.0d;
                                    try {
                                        if (!pVal.isEmpty()) {
                                            pDouble = Double.parseDouble(pVal);
                                        }
                                    } catch (Exception e) {}
                                    item.setPrice(pDouble);
                                    if (runnableArr4[0] != null) {
                                        runnableArr4[0].run();
                                    }
                                    linearLayout3.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (runnableArr5[0] != null) {
                                                runnableArr5[0].run();
                                            }
                                        }
                                    });
                                }
                            })
                            .setNegativeButton("বাতিল", null)
                            .show();
                    }
                });
                final FordiModel fordiModel = this.val$fordi;
                final Runnable[] runnableArr6 = this.val$saveCurrentState;
                final Runnable[] runnableArr7 = this.val$populateItemsList;
                btnRowDel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        fordiModel.getItems().remove(item);
                        if (runnableArr6[0] != null) {
                            runnableArr6[0].run();
                        }
                        if (runnableArr7[0] != null) {
                            runnableArr7[0].run();
                        }
                    }
                });
                materialCardView.addView(linearLayout);
                this.val$itemsListContainer.addView(materialCardView);
                i = 0;
            }
        }

    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showFordiDetailDialog$73$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7042lambda$showFordiDetailDialog$73$comexampleMainActivity(TextInputEditText etItemName, TextInputEditText etItemPrice, TextInputLayout tilItemName, TextInputLayout tilItemPrice, FordiModel fordi, Runnable[] saveCurrentState, Runnable[] populateItemsList, View v) {
        double pVal;
        String name = etItemName.getText().toString().trim();
        String pStr = etItemPrice.getText().toString().trim();
        if (name.isEmpty()) {
            tilItemName.setError("পণ্যের নাম দিন");
            return;
        }
        tilItemName.setError(null);
        try {
            if (pStr.isEmpty()) {
                pVal = 0.0d;
            } else {
                double pVal2 = Double.parseDouble(pStr);
                pVal = pVal2;
            }
            tilItemPrice.setError(null);
            String itemId = UUID.randomUUID().toString();
            FordiItemModel newItem = new FordiItemModel(itemId, name, false, pVal);
            fordi.getItems().add(newItem);
            etItemName.setText("");
            etItemPrice.setText("");
            etItemName.requestFocus();
            saveCurrentState[0].run();
            populateItemsList[0].run();
            Toast.makeText(this, "✅ পণ্যটি ফর্দে যুক্ত হয়েছে!", 0).show();
        } catch (Exception e) {
            tilItemPrice.setError("সঠিক সংখ্যা");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showFordiDetailDialog$75$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7044lambda$showFordiDetailDialog$75$comexampleMainActivity(final FordiModel fordi, final Runnable[] saveCurrentState, final LinearLayout dialogRoot, View v) {
        String[] names = {"⚪ হালকা ধূসর", "🧡 কমলা আভা", "💚 পুদিনা সবুজ", "💙 আকাশী নীল", "💜 বেগুনী আভা", "💗 গোলাপী আভা"};
        final String[] codes = {"#F1F5F9", "#FFF7ED", "#ECFDF5", "#EFF6FF", "#FAF5FF", "#FDF2F8"};
        new MaterialAlertDialogBuilder(this).setTitle((CharSequence) "🎨 ফর্দের ব্যাকগ্রাউন্ড থিম").setItems((CharSequence[]) names, new DialogInterface.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda5
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m7043lambda$showFordiDetailDialog$74$comexampleMainActivity(codes, fordi, saveCurrentState, dialogRoot, dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showFordiDetailDialog$74$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7043lambda$showFordiDetailDialog$74$comexampleMainActivity(String[] codes, FordiModel fordi, Runnable[] saveCurrentState, LinearLayout dialogRoot, DialogInterface d, int which) {
        String selectedColor = codes[which];
        fordi.setColorHex(selectedColor);
        saveCurrentState[0].run();
        dialogRoot.setBackgroundColor(Color.parseColor(selectedColor));
        Toast.makeText(this, "🎨 ব্যাকগ্রাউন্ড থিম পরিবর্তন করা হয়েছে!", 0).show();
    }

    private void shareFordiList(FordiModel fordi) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 ").append(fordi.getTitle()).append(" - বাজার করার ফর্দ\n");
        sb.append("-----------------------------\n");
        double total = 0.0d;
        int checked = 0;
        for (FordiItemModel item : fordi.getItems()) {
            total += item.getPrice();
            String status = item.isChecked() ? "✅ " : "⬜ ";
            sb.append(status).append(item.getName());
            if (item.getPrice() > 0.0d) {
                sb.append(" (৳ ").append(String.format(Locale.getDefault(), "%,.0f", Double.valueOf(item.getPrice()))).append(")");
            }
            sb.append("\n");
            if (item.isChecked()) {
                checked++;
            }
        }
        sb.append("-----------------------------\n");
        sb.append("📊 মোট বাজেট: ৳ ").append(String.format(Locale.getDefault(), "%,.0f", Double.valueOf(total))).append("\n");
        sb.append("🛒 কেনা হয়েছে: ").append(checked).append("/").append(fordi.getItems().size()).append(" টি পণ্য\n\n");
        sb.append("ধন্যবাদান্তে,\nআমার ক্যাশ খাতা");
        String msg = sb.toString();
        ClipboardManager clipboard = (ClipboardManager) getSystemService("clipboard");
        ClipData clip = ClipData.newPlainText("Shopping List", msg);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "✅ ফর্দটি ক্লিপবোর্ডে কপি করা হয়েছে!", 0).show();
        Intent intent = new Intent();
        intent.setAction("android.intent.action.SEND");
        intent.putExtra("android.intent.extra.TEXT", msg);
        intent.setType("text/plain");
        startActivity(Intent.createChooser(intent, "ফর্দটি শেয়ার করুন"));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onPause() {
        super.onPause();
        this.backupHandler.removeCallbacks(this.backupRunnable);
        triggerAutoCloudBackup();
    }
}
