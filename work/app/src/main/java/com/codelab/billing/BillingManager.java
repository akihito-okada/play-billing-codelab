/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codelab.billing;

import android.app.Activity;
import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * TODO: Implement BillingManager that will handle all the interactions with Play Store
 * (via Billing library), maintain connection to it through BillingClient and cache
 * temporary states/data if needed.
 */
public class BillingManager implements PurchasesUpdatedListener {
    private static final String TAG = "BillingManager";
    private final BillingClient mBillingClient;
    private final Activity mActivity;

    private static final HashMap<String, List<String>> SKUS;
    static
    {
        SKUS = new HashMap<>();
        SKUS.put(BillingClient.SkuType.INAPP, Arrays.asList("gas", "premium"));
        SKUS.put(BillingClient.SkuType.SUBS, Arrays.asList("gold_monthly", "gold_yearly"));
    }

    public List<String> getSkus(@BillingClient.SkuType String type) {
        return SKUS.get(type);
    }

    public BillingManager(Activity activity) {
        mActivity = activity;
        mBillingClient = BillingClient.newBuilder(activity).setListener(this).build();
    }

    private void startServiceConnectionIfNeeded(final Runnable executeOnSuccess) {
        if (mBillingClient.isReady()) {
            if (executeOnSuccess == null) {
                return;
            }
            executeOnSuccess.run();
            return;
        }
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponse) {
                if (billingResponse == BillingClient.BillingResponse.OK) {
                    Log.i(TAG, "onBillingSetupFinished() response: " + billingResponse);
                    if (executeOnSuccess == null) {
                        return;
                    }
                    executeOnSuccess.run();
                    return;
                }
                Log.w(TAG, "onBillingSetupFinished() error code: " + billingResponse);
                }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG, "onBillingServiceDisconnected()");
            }
        });
    }

    public void startPurchaseFlow(final String skuId, final String billingType) {
        // Specify a runnable ti start when connection  to Billong client is established
        Runnable executeOnConnectedService = new Runnable() {
            @Override
            public void run() {
                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder().setType(billingType).setSku(skuId).build();
                mBillingClient.launchBillingFlow(mActivity, billingFlowParams);
            }
        };
        // If Billing client was disconnected, we retry 1 time
        // and if success, execute the query
        startServiceConnectionIfNeeded(executeOnConnectedService);
    }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
        Log.d(TAG, "onPurchasesUpdated() response: " + responseCode);
    }

    public void querySkuDetailsAsync(@BillingClient.SkuType final  String itemType, final List<String> skuList, final SkuDetailsResponseListener listener) {
        Runnable executeOnConnectedService = new Runnable() {
            @Override
            public void run() {
                SkuDetailsParams skuDetailsParams = SkuDetailsParams.newBuilder().setSkusList(skuList).setType(itemType).build();
                mBillingClient.querySkuDetailsAsync(skuDetailsParams, new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(int responseCode, List<SkuDetails> skuDetailsList) {
                        listener.onSkuDetailsResponse(responseCode, skuDetailsList);
                    }
                });
            }
        };
        // If Billing client disconnected, we retry 1 time
        // and if success, execute the query
        startServiceConnectionIfNeeded(executeOnConnectedService);
    }

    public void destroy() {
        mBillingClient.endConnection();
    }
}
