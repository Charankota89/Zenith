package com.zenith.app.ui.screen;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class ScreenViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;
    public ScreenViewModelFactory(Context ctx) { this.context = ctx.getApplicationContext(); }
    @NonNull @Override @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> c) { return (T) new ScreenViewModel(context); }
}
