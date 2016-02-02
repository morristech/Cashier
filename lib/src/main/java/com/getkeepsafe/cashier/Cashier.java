package com.getkeepsafe.cashier;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.getkeepsafe.cashier.googleplay.InAppBillingV3Vendor;
import com.getkeepsafe.cashier.logging.Logger;
import com.getkeepsafe.cashier.utilities.Check;

public class Cashier {
    private final Activity activity;
    private final Vendor vendor;

    public static Builder forGooglePlay(@NonNull final Activity activity) {
        return forGooglePlay(activity, null);
    }
    public static Builder forGooglePlay(@NonNull final Activity activity,
                                        @Nullable final String developerPayload) {
        return new Builder(activity).forVendor(
                new InAppBillingV3Vendor(activity.getPackageName(), developerPayload));
    }

    private Cashier(@NonNull final Activity activity,
                   @NonNull final Vendor vendor) {
        this.activity = Check.notNull(activity, "Activity");
        this.vendor = Check.notNull(vendor, "Vendor");
    }

    public void purchase(@NonNull final Product product, @NonNull final PurchaseListener listener) {
        Check.notNull(product, "Product");
        Check.notNull(listener, "Listener");
        vendor.initialize(activity, new Vendor.InitializationListener() {
            @Override
            public void initialized() {
                if (!vendor.available() || !vendor.canPurchase(product)) {
                    listener.failure(product, Vendor.PURCHASE_UNAVAILABLE);
                    return;
                }

                vendor.purchase(activity, product, listener);
            }
        });
    }

    public void consume(@NonNull final Purchase purchase, @NonNull final ConsumeListener listener) {
        Check.notNull(purchase, "Purchase");
        Check.notNull(listener, "Listener");
        if (purchase.isSubscription) {
            throw new IllegalArgumentException("Cannot consume a subscription type!");
        }

        vendor.initialize(activity, new Vendor.InitializationListener() {
            @Override
            public void initialized() {
                vendor.consume(activity, purchase, listener);
            }
        });
    }

    public void dispose() {
        vendor.dispose(activity);
    }

    public boolean onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        return vendor.onActivityResult(requestCode, resultCode, data);
    }

    public static class Builder {
        private final Activity activity;
        private Vendor vendor;
        private Logger logger;

        public Builder(final Activity activity) {
            this.activity = Check.notNull(activity, "Activity");
        }

        public Builder forVendor(final Vendor vendor) {
            this.vendor = vendor;
            return this;
        }

        public Builder withLogger(final Logger logger) {
            this.logger = logger;
            return this;
        }

        public Cashier build() {
            if (logger != null) {
                vendor.setLogger(logger);
            }

            return new Cashier(activity, vendor);
        }
    }
}
