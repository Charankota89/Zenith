package com.zenith.app.ui.wellbeing;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class WellbeingViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;

    public WellbeingViewModelFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new WellbeingViewModel(context);
    }
}
