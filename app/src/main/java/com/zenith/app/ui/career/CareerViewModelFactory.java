package com.zenith.app.ui.career;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class CareerViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;
    public CareerViewModelFactory(Context ctx) { this.context = ctx.getApplicationContext(); }
    @NonNull @Override @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> c) { return (T) new CareerViewModel(context); }
}
