/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.internal.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.android.internal.app.ChooserWrapperActivity.sOverrides;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.usage.UsageStatsManager;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.android.internal.R;
import com.android.internal.app.ResolverActivity.ResolvedComponentInfo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * Chooser activity instrumentation tests
 */
@RunWith(AndroidJUnit4.class)
public class ChooserActivityTest {
    @Rule
    public ActivityTestRule<ChooserWrapperActivity> mActivityRule =
            new ActivityTestRule<>(ChooserWrapperActivity.class, false,
                    false);

    @Before
    public void cleanOverrideData() {
        sOverrides.reset();
    }

    @Test
    public void customTitle() throws InterruptedException {
        Intent viewIntent = createViewTextIntent();
        List<ResolvedComponentInfo> resolvedComponentInfos = createResolvedComponentsForTest(2);

        when(sOverrides.resolverListController.getResolversForIntent(Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.isA(List.class))).thenReturn(resolvedComponentInfos);
        mActivityRule.launchActivity(Intent.createChooser(viewIntent, "chooser test"));

        waitForIdle();
        onView(withId(R.id.title)).check(matches(withText("chooser test")));
    }

    @Test
    public void customTitleIgnoredForSendIntents() throws InterruptedException {
        Intent sendIntent = createSendTextIntent();
        List<ResolvedComponentInfo> resolvedComponentInfos = createResolvedComponentsForTest(2);

        when(sOverrides.resolverListController.getResolversForIntent(Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.isA(List.class))).thenReturn(resolvedComponentInfos);
        mActivityRule.launchActivity(Intent.createChooser(sendIntent, "chooser test"));
        waitForIdle();
        onView(withId(R.id.title)).check(matches(withText(R.string.whichSendApplication)));
    }

    @Test
    public void emptyTitle() throws InterruptedException {
        Intent sendIntent = createSendTextIntent();
        List<ResolvedComponentInfo> resolvedComponentInfos = createResolvedComponentsForTest(2);

        when(sOverrides.resolverListController.getResolversForIntent(Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.isA(List.class))).thenReturn(resolvedComponentInfos);
        mActivityRule.launchActivity(Intent.createChooser(sendIntent, null));
        waitForIdle();
        onView(withId(R.id.title))
                .check(matches(withText(R.string.whichSendApplication)));
    }

    @Test
    public void emptyPreviewTitleAndThumbnail() throws InterruptedException {
        Intent sendIntent = createSendTextIntentWithPreview(null, null);
        List<ResolvedComponentInfo> resolvedComponentInfos = createResolvedComponentsForTest(2);

        when(sOverrides.resolverListController.getResolversForIntent(Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.isA(List.class))).thenReturn(resolvedComponentInfos);
        mActivityRule.launchActivity(Intent.createChooser(sendIntent, null));
        waitForIdle();
        onView(withId(R.id.content_preview_title)).check(matches(not(isDisplayed())));
        onView(withId(R.id.content_preview_thumbnail)).check(matches(not(isDisplayed())));
    }

    @Test
    public void visiblePreviewTitleWithoutThumbnail() throws InterruptedException {
        String previewTitle = "My Content Preview Title";
        Intent sendIntent = createSendTextIntentWithPreview(previewTitle, null);
        List<ResolvedComponentInfo> resolvedComponentInfos = createResolvedComponentsForTest(2);

        when(sOverrides.resolverListController.getResolversForIntent(Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.isA(List.class))).thenReturn(resolvedComponentInfos);
        mActivityRule.launchActivity(Intent.createChooser(sendIntent, null));
        waitForIdle();
        onView(withId(R.id.content_preview_title)).check(matches(isDisplayed()));
        onView(withId(R.id.content_preview_title)).check(matches(withText(previewTitle)));
        onView(withId(R.id.content_preview_thumbnail)).check(matches(not(isDisplayed())));
    }

    @Test
    public void visiblePreviewTitleWithInvalidThumbnail() throws InterruptedException {
        String previewTitle = "My Content Preview Title";
        Intent sendIntent = createSendTextIntentWithPreview(previewTitle,
                Uri.parse("tel:(+49)12345789"));
        List<ResolvedComponentInfo> resolvedComponentInfos = createResolvedComponentsForTest(2);

        when(sOverrides.resolverListController.getResolversForIntent(Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.isA(List.class))).thenReturn(resolvedComponentInfos);
        mActivityRule.launchActivity(Intent.createChooser(sendIntent, null));
        waitForIdle();
        onView(withId(R.id.content_preview_title)).check(matches(isDisplayed()));
        onView(withId(R.id.content_preview_thumbnail)).check(matches(not(isDisplayed())));
    }

    @Test
    public void visiblePreviewTitleAndThumbnail() throws InterruptedException {
        String previewTitle = "My Content Preview Title";
        Intent sendIntent = createSendTextIntentWithPreview(previewTitle,
                Uri.parse("android.resource://com.android.frameworks.coretests/"
                        + com.android.frameworks.coretests.R.drawable.test320x240));
        sOverrides.previewThumbnail = createBitmap();
        List<ResolvedComponentInfo> resolvedComponentInfos = createResolvedComponentsForTest(2);

        when(sOverrides.resolverListController.getResolversForIntent(Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.isA(List.class))).thenReturn(resolvedComponentInfos);
        mActivityRule.launchActivity(Intent.createChooser(sendIntent, null));
        waitForIdle();
        onView(withId(R.id.content_preview_title)).check(matches(isDisplayed()));
        onView(withId(R.id.content_preview_thumbnail)).check(matches(isDisplayed()));
    }

    @Test
    public void twoOptionsAndUserSelectsOne() throws InterruptedException {
        Intent sendIntent = createSendTextIntent();
        List<ResolvedComponentInfo> resolvedComponentInfos = createResolvedComponentsForTest(2);

        when(sOverrides.resolverListController.getResolversForIntent(Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.isA(List.class))).thenReturn(resolvedComponentInfos);

        final ChooserWrapperActivity activity = mActivityRule
                .launchActivity(Intent.createChooser(sendIntent, null));
        waitForIdle();

        assertThat(activity.getAdapter().getCount(), is(2));
        onView(withId(R.id.profile_button)).check(matches(not(isDisplayed())));

        ResolveInfo[] chosen = new ResolveInfo[1];
        sOverrides.onSafelyStartCallback = targetInfo -> {
            chosen[0] = targetInfo.getResolveInfo();
            return true;
        };

        ResolveInfo toChoose = resolvedComponentInfos.get(0).getResolveInfoAt(0);
        onView(withText(toChoose.activityInfo.name))
                .perform(click());
        waitForIdle();
        assertThat(chosen[0], is(toChoose));
    }

    @Test
    public void updateChooserCountsAndModelAfterUserSelection() throws InterruptedException {
        Intent sendIntent = createSendTextIntent();
        List<ResolvedComponentInfo> resolvedComponentInfos = createResolvedComponentsForTest(2);

        when(sOverrides.resolverListController.getResolversForIntent(Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.isA(List.class))).thenReturn(resolvedComponentInfos);

        final ChooserWrapperActivity activity = mActivityRule
                .launchActivity(Intent.createChooser(sendIntent, null));
        waitForIdle();
        UsageStatsManager usm = activity.getUsageStatsManager();
        verify(sOverrides.resolverListController, times(1))
                .sort(Mockito.any(List.class));
        assertThat(activity.getIsSelected(), is(false));
        sOverrides.onSafelyStartCallback = targetInfo -> {
            return true;
        };
        ResolveInfo toChoose = resolvedComponentInfos.get(0).getResolveInfoAt(0);
        onView(withText(toChoose.activityInfo.name))
                .perform(click());
        waitForIdle();
        verify(sOverrides.resolverListController, times(1))
                .updateChooserCounts(Mockito.anyString(), Mockito.anyInt(), Mockito.anyString());
        verify(sOverrides.resolverListController, times(1))
                .updateModel(toChoose.activityInfo.getComponentName());
        assertThat(activity.getIsSelected(), is(true));
    }

    @Test
    public void noResultsFromPackageManager() {
        when(sOverrides.resolverListController.getResolversForIntent(Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.isA(List.class))).thenReturn(null);
        Intent sendIntent = createSendTextIntent();
        final ChooserWrapperActivity activity = mActivityRule
                .launchActivity(Intent.createChooser(sendIntent, null));
        waitForIdle();
        assertThat(activity.isFinishing(), is(false));

        onView(withId(R.id.empty)).check(matches(isDisplayed()));
        onView(withId(R.id.resolver_list)).check(matches(not(isDisplayed())));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> activity.getAdapter().handlePackagesChanged()
        );
        // backward compatibility. looks like we finish when data is empty after package change
        assertThat(activity.isFinishing(), is(true));
    }

    @Test
    public void autoLaunchSingleResult() throws InterruptedException {
        ResolveInfo[] chosen = new ResolveInfo[1];
        sOverrides.onSafelyStartCallback = targetInfo -> {
            chosen[0] = targetInfo.getResolveInfo();
            return true;
        };

        List<ResolvedComponentInfo> resolvedComponentInfos = createResolvedComponentsForTest(1);
        when(sOverrides.resolverListController.getResolversForIntent(Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.isA(List.class))).thenReturn(resolvedComponentInfos);

        Intent sendIntent = createSendTextIntent();
        final ChooserWrapperActivity activity = mActivityRule
                .launchActivity(Intent.createChooser(sendIntent, null));
        waitForIdle();

        assertThat(chosen[0], is(resolvedComponentInfos.get(0).getResolveInfoAt(0)));
        assertThat(activity.isFinishing(), is(true));
    }

    @Test
    public void hasOtherProfileOneOption() throws Exception {
        Intent sendIntent = createSendTextIntent();
        List<ResolvedComponentInfo> resolvedComponentInfos =
                createResolvedComponentsForTestWithOtherProfile(2);
        ResolveInfo toChoose = resolvedComponentInfos.get(1).getResolveInfoAt(0);

        when(ChooserWrapperActivity.sOverrides.resolverListController.getResolversForIntent(
                Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.isA(List.class))).thenReturn(resolvedComponentInfos);

        final ChooserWrapperActivity activity = mActivityRule
                .launchActivity(Intent.createChooser(sendIntent, null));
        waitForIdle();

        // The other entry is filtered to the other profile slot
        assertThat(activity.getAdapter().getCount(), is(1));

        ResolveInfo[] chosen = new ResolveInfo[1];
        ChooserWrapperActivity.sOverrides.onSafelyStartCallback = targetInfo -> {
            chosen[0] = targetInfo.getResolveInfo();
            return true;
        };

        // Make a stable copy of the components as the original list may be modified
        List<ResolvedComponentInfo> stableCopy =
                createResolvedComponentsForTestWithOtherProfile(2);
        // Check that the "Other Profile" activity is put in the right spot
        onView(withId(R.id.profile_button)).check(matches(
                withText(stableCopy.get(0).getResolveInfoAt(0).activityInfo.name)));
        onView(withText(stableCopy.get(1).getResolveInfoAt(0).activityInfo.name))
                .perform(click());
        waitForIdle();
        assertThat(chosen[0], is(toChoose));
    }

    @Test
    public void hasOtherProfileTwoOptionsAndUserSelectsOne() throws Exception {
        Intent sendIntent = createSendTextIntent();
        List<ResolvedComponentInfo> resolvedComponentInfos =
                createResolvedComponentsForTestWithOtherProfile(3);
        ResolveInfo toChoose = resolvedComponentInfos.get(1).getResolveInfoAt(0);

        when(ChooserWrapperActivity.sOverrides.resolverListController.getResolversForIntent(
                Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.isA(List.class))).thenReturn(resolvedComponentInfos);
        when(ChooserWrapperActivity.sOverrides.resolverListController.getLastChosen())
                .thenReturn(resolvedComponentInfos.get(0).getResolveInfoAt(0));

        final ChooserWrapperActivity activity = mActivityRule
                .launchActivity(Intent.createChooser(sendIntent, null));
        waitForIdle();

        // The other entry is filtered to the other profile slot
        assertThat(activity.getAdapter().getCount(), is(2));

        ResolveInfo[] chosen = new ResolveInfo[1];
        ChooserWrapperActivity.sOverrides.onSafelyStartCallback = targetInfo -> {
            chosen[0] = targetInfo.getResolveInfo();
            return true;
        };

        // Make a stable copy of the components as the original list may be modified
        List<ResolvedComponentInfo> stableCopy =
                createResolvedComponentsForTestWithOtherProfile(3);
        // Check that the "Other Profile" activity is put in the right spot
        onView(withId(R.id.profile_button)).check(matches(
                withText(stableCopy.get(0).getResolveInfoAt(0).activityInfo.name)));
        onView(withText(stableCopy.get(1).getResolveInfoAt(0).activityInfo.name))
                .perform(click());
        waitForIdle();
        assertThat(chosen[0], is(toChoose));
    }

    @Test
    public void hasLastChosenActivityAndOtherProfile() throws Exception {
        Intent sendIntent = createSendTextIntent();
        List<ResolvedComponentInfo> resolvedComponentInfos =
                createResolvedComponentsForTestWithOtherProfile(3);
        ResolveInfo toChoose = resolvedComponentInfos.get(1).getResolveInfoAt(0);

        when(ChooserWrapperActivity.sOverrides.resolverListController.getResolversForIntent(
                Mockito.anyBoolean(),
                Mockito.anyBoolean(),
                Mockito.isA(List.class))).thenReturn(resolvedComponentInfos);

        final ChooserWrapperActivity activity = mActivityRule
                .launchActivity(Intent.createChooser(sendIntent, null));
        waitForIdle();

        // The other entry is filtered to the last used slot
        assertThat(activity.getAdapter().getCount(), is(2));

        ResolveInfo[] chosen = new ResolveInfo[1];
        ChooserWrapperActivity.sOverrides.onSafelyStartCallback = targetInfo -> {
            chosen[0] = targetInfo.getResolveInfo();
            return true;
        };

        // Make a stable copy of the components as the original list may be modified
        List<ResolvedComponentInfo> stableCopy =
                createResolvedComponentsForTestWithOtherProfile(3);
        // Check that the "Other Profile" activity is put in the right spot
        onView(withId(R.id.profile_button)).check(matches(
                withText(stableCopy.get(0).getResolveInfoAt(0).activityInfo.name)));
        onView(withText(stableCopy.get(1).getResolveInfoAt(0).activityInfo.name))
                .perform(click());
        waitForIdle();
        assertThat(chosen[0], is(toChoose));
    }

    private Intent createSendTextIntent() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "testing intent sending");
        return sendIntent;
    }

    private Intent createSendTextIntentWithPreview(String title, Uri imageThumbnail) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "testing intent sending");
        sendIntent.putExtra(Intent.EXTRA_TITLE, title);
        if (imageThumbnail != null) {
            ClipData.Item clipItem = new ClipData.Item(imageThumbnail);
            sendIntent.setClipData(new ClipData("Clip Label", new String[]{"image/png"}, clipItem));
        }

        return sendIntent;
    }

    private Intent createViewTextIntent() {
        Intent viewIntent = new Intent();
        viewIntent.setAction(Intent.ACTION_VIEW);
        viewIntent.putExtra(Intent.EXTRA_TEXT, "testing intent viewing");
        return viewIntent;
    }

    private List<ResolvedComponentInfo> createResolvedComponentsForTest(int numberOfResults) {
        List<ResolvedComponentInfo> infoList = new ArrayList<>(numberOfResults);
        for (int i = 0; i < numberOfResults; i++) {
            infoList.add(ResolverDataProvider.createResolvedComponentInfo(i));
        }
        return infoList;
    }

    private List<ResolvedComponentInfo> createResolvedComponentsForTestWithOtherProfile(
            int numberOfResults) {
        List<ResolvedComponentInfo> infoList = new ArrayList<>(numberOfResults);
        for (int i = 0; i < numberOfResults; i++) {
            if (i == 0) {
                infoList.add(ResolverDataProvider.createResolvedComponentInfoWithOtherId(i));
            } else {
                infoList.add(ResolverDataProvider.createResolvedComponentInfo(i));
            }
        }
        return infoList;
    }

    private void waitForIdle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private Bitmap createBitmap() {
        int width = 200;
        int height = 200;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPaint(paint);

        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setTextSize(14.f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Hi!", (width / 2.f), (height / 2.f), paint);

        return bitmap;
    }
}
