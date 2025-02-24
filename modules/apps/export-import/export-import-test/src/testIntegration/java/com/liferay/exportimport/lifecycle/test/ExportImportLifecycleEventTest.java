/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.exportimport.lifecycle.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.exportimport.kernel.configuration.ExportImportConfigurationParameterMapFactoryUtil;
import com.liferay.exportimport.kernel.configuration.ExportImportConfigurationSettingsMapFactoryUtil;
import com.liferay.exportimport.kernel.configuration.constants.ExportImportConfigurationConstants;
import com.liferay.exportimport.kernel.lifecycle.ExportImportLifecycleEvent;
import com.liferay.exportimport.kernel.lifecycle.ExportImportLifecycleEventListenerRegistryUtil;
import com.liferay.exportimport.kernel.lifecycle.ExportImportLifecycleListener;
import com.liferay.exportimport.kernel.lifecycle.constants.ExportImportLifecycleConstants;
import com.liferay.exportimport.kernel.model.ExportImportConfiguration;
import com.liferay.exportimport.kernel.service.ExportImportConfigurationLocalServiceUtil;
import com.liferay.exportimport.kernel.service.ExportImportLocalServiceUtil;
import com.liferay.exportimport.kernel.staging.StagingUtil;
import com.liferay.journal.constants.JournalFolderConstants;
import com.liferay.journal.test.util.JournalTestUtil;
import com.liferay.layout.test.util.LayoutTestUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.NoSuchGroupException;
import com.liferay.portal.kernel.exception.NoSuchLayoutException;
import com.liferay.portal.kernel.exception.NoSuchLayoutSetException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.rule.Sync;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.TimeZoneUtil;
import com.liferay.portal.test.log.LogCapture;
import com.liferay.portal.test.log.LogEntry;
import com.liferay.portal.test.log.LoggerTestUtil;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;

import java.io.File;
import java.io.Serializable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Daniel Kocsis
 */
@RunWith(Arquillian.class)
@Sync(cleanTransaction = true)
public class ExportImportLifecycleEventTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		UserTestUtil.setUser(TestPropsValues.getUser());

		_group = GroupTestUtil.addGroup();
		_liveGroup = GroupTestUtil.addGroup();

		ExportImportLifecycleEventListenerRegistryUtil.register(
			new MockExportImportLifecycleListener());

		_parameterMap =
			ExportImportConfigurationParameterMapFactoryUtil.
				buildParameterMap();
	}

	@Test
	public void testFailedLayoutExport() throws Exception {
		Map<String, Serializable> exportLayoutSettingsMap =
			ExportImportConfigurationSettingsMapFactoryUtil.
				buildExportLayoutSettingsMap(
					TestPropsValues.getUserId(), 0, false, new long[0],
					_parameterMap, LocaleUtil.US, TimeZoneUtil.GMT);

		ExportImportConfiguration exportImportConfiguration =
			ExportImportConfigurationLocalServiceUtil.
				addDraftExportImportConfiguration(
					TestPropsValues.getUserId(),
					ExportImportConfigurationConstants.TYPE_EXPORT_LAYOUT,
					exportLayoutSettingsMap);

		try {
			ExportImportLocalServiceUtil.exportLayoutsAsFile(
				exportImportConfiguration);

			Assert.fail();
		}
		catch (NoSuchGroupException noSuchGroupException) {
			Assert.assertEquals(
				"No Group exists with the primary key 0",
				noSuchGroupException.getMessage());
		}
		catch (NoSuchLayoutSetException noSuchLayoutSetException) {
			Assert.assertEquals(
				"No LayoutSet exists with the key {groupId=0, " +
					"privateLayout=false}",
				noSuchLayoutSetException.getMessage());
		}

		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.EVENT_LAYOUT_EXPORT_FAILED));
	}

	@Test
	public void testFailedLayoutImport() throws Exception {
		Map<String, Serializable> importLayoutSettingsMap =
			ExportImportConfigurationSettingsMapFactoryUtil.
				buildImportLayoutSettingsMap(
					TestPropsValues.getUserId(), 0, false, new long[0],
					_parameterMap, LocaleUtil.US, TimeZoneUtil.GMT);

		ExportImportConfiguration exportImportConfiguration =
			ExportImportConfigurationLocalServiceUtil.
				addDraftExportImportConfiguration(
					TestPropsValues.getUserId(),
					ExportImportConfigurationConstants.TYPE_IMPORT_LAYOUT,
					importLayoutSettingsMap);

		try {
			ExportImportLocalServiceUtil.importLayouts(
				exportImportConfiguration, (File)null);

			Assert.fail();
		}
		catch (NoSuchGroupException noSuchGroupException) {
			Assert.assertEquals(
				"No Group exists with the primary key 0",
				noSuchGroupException.getMessage());
		}

		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.EVENT_LAYOUT_IMPORT_FAILED));
	}

	@Test
	public void testFailedLayoutLocalPublishing() throws Exception {
		try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				"com.liferay.portal.background.task.internal.messaging." +
					"BackgroundTaskMessageListener",
				LoggerTestUtil.ERROR)) {

			long targetGroupId = RandomTestUtil.nextLong();

			StagingUtil.publishLayouts(
				TestPropsValues.getUserId(), _group.getGroupId(), targetGroupId,
				false, new long[0], _parameterMap);

			List<LogEntry> logEntries = logCapture.getLogEntries();

			LogEntry logEntry = logEntries.get(0);

			Assert.assertEquals(
				"Unable to execute background task", logEntry.getMessage());

			Throwable throwable = logEntry.getThrowable();

			Assert.assertSame(NoSuchGroupException.class, throwable.getClass());
		}

		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.
					EVENT_PUBLICATION_LAYOUT_LOCAL_FAILED));
	}

	@Test
	public void testFailedPortletExport() throws Exception {
		long plid = RandomTestUtil.nextLong();

		Map<String, Serializable> exportPortletSettingsMap =
			ExportImportConfigurationSettingsMapFactoryUtil.
				buildExportPortletSettingsMap(
					TestPropsValues.getUserId(), plid, _group.getGroupId(),
					StringPool.BLANK, _parameterMap, LocaleUtil.US,
					TimeZoneUtil.GMT, StringPool.BLANK);

		ExportImportConfiguration exportImportConfiguration =
			ExportImportConfigurationLocalServiceUtil.
				addDraftExportImportConfiguration(
					TestPropsValues.getUserId(),
					ExportImportConfigurationConstants.TYPE_EXPORT_PORTLET,
					exportPortletSettingsMap);

		try {
			ExportImportLocalServiceUtil.exportPortletInfoAsFile(
				exportImportConfiguration);

			Assert.fail();
		}
		catch (NoSuchLayoutException noSuchLayoutException) {
			Assert.assertEquals(
				"No Layout exists with the primary key " + plid,
				noSuchLayoutException.getMessage());
		}

		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.EVENT_PORTLET_EXPORT_FAILED));
	}

	@Test
	public void testFailedPortletImport() throws Exception {
		Map<String, Serializable> importPortletSettingsMap =
			ExportImportConfigurationSettingsMapFactoryUtil.
				buildImportPortletSettingsMap(
					TestPropsValues.getUserId(), 0, _group.getGroupId(),
					StringPool.BLANK, _parameterMap, LocaleUtil.US,
					TimeZoneUtil.GMT);

		ExportImportConfiguration exportImportConfiguration =
			ExportImportConfigurationLocalServiceUtil.
				addDraftExportImportConfiguration(
					TestPropsValues.getUserId(),
					ExportImportConfigurationConstants.TYPE_IMPORT_PORTLET,
					importPortletSettingsMap);

		try {
			ExportImportLocalServiceUtil.importPortletInfo(
				exportImportConfiguration, (File)null);

			Assert.fail();
		}
		catch (NoSuchLayoutException noSuchLayoutException) {
			Assert.assertEquals(
				"No Layout exists with the primary key 0",
				noSuchLayoutException.getMessage());
		}

		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.EVENT_PORTLET_IMPORT_FAILED));
	}

	@Test
	public void testFailedPortletLocalPublishing() throws Exception {
		User user = TestPropsValues.getUser();

		try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				"com.liferay.portal.background.task.internal.messaging." +
					"BackgroundTaskMessageListener",
				LoggerTestUtil.ERROR)) {

			StagingUtil.publishPortlet(
				user.getUserId(), _group.getGroupId(), _liveGroup.getGroupId(),
				0, 0, StringPool.BLANK, _parameterMap);

			List<LogEntry> logEntries = logCapture.getLogEntries();

			LogEntry logEntry = logEntries.get(0);

			Assert.assertEquals(
				"Unable to execute background task", logEntry.getMessage());

			Throwable throwable = logEntry.getThrowable();

			Assert.assertSame(
				NoSuchLayoutException.class, throwable.getClass());
		}

		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.
					EVENT_PUBLICATION_PORTLET_LOCAL_FAILED));
	}

	@Test
	public void testSuccessfulLayoutLocalPublishing() throws Exception {
		LayoutTestUtil.addLayout(_group, false);

		JournalTestUtil.addArticle(
			_group.getGroupId(),
			JournalFolderConstants.DEFAULT_PARENT_FOLDER_ID);

		StagingUtil.publishLayouts(
			TestPropsValues.getUserId(), _group.getGroupId(),
			_liveGroup.getGroupId(), false, null, _parameterMap);

		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.EVENT_LAYOUT_EXPORT_STARTED));
		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.EVENT_LAYOUT_EXPORT_SUCCEEDED));
		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.EVENT_LAYOUT_IMPORT_STARTED));
		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.EVENT_LAYOUT_IMPORT_SUCCEEDED));
		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.EVENT_PORTLET_EXPORT_STARTED));
		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.EVENT_PORTLET_EXPORT_SUCCEEDED));
		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.EVENT_PORTLET_IMPORT_STARTED));
		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.EVENT_PORTLET_IMPORT_SUCCEEDED));
		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.
					EVENT_PUBLICATION_LAYOUT_LOCAL_STARTED));
		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.
					EVENT_PUBLICATION_LAYOUT_LOCAL_SUCCEEDED));
		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.
					EVENT_STAGED_MODEL_EXPORT_STARTED));
		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.
					EVENT_STAGED_MODEL_EXPORT_SUCCEEDED));
		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.
					EVENT_STAGED_MODEL_IMPORT_STARTED));
		Assert.assertTrue(
			_firedExportImportLifecycleEventsMap.containsKey(
				ExportImportLifecycleConstants.
					EVENT_STAGED_MODEL_IMPORT_SUCCEEDED));
	}

	private final Map<Integer, ExportImportLifecycleEvent>
		_firedExportImportLifecycleEventsMap = new HashMap<>();

	@DeleteAfterTestRun
	private Group _group;

	@DeleteAfterTestRun
	private Group _liveGroup;

	private Map<String, String[]> _parameterMap;

	private class MockExportImportLifecycleListener
		implements ExportImportLifecycleListener {

		@Override
		public boolean isParallel() {
			return false;
		}

		@Override
		public void onExportImportLifecycleEvent(
				ExportImportLifecycleEvent exportImportLifecycleEvent)
			throws Exception {

			Assert.assertNotNull(exportImportLifecycleEvent);

			_firedExportImportLifecycleEventsMap.put(
				exportImportLifecycleEvent.getCode(),
				exportImportLifecycleEvent);
		}

	}

}