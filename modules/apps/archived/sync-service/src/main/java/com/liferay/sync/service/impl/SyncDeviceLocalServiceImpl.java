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

package com.liferay.sync.service.impl;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.aop.AopService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.sync.constants.SyncDeviceConstants;
import com.liferay.sync.model.SyncDevice;
import com.liferay.sync.service.base.SyncDeviceLocalServiceBaseImpl;

import java.util.Date;
import java.util.List;

import org.osgi.service.component.annotations.Component;

/**
 * @author Shinn Lok
 */
@Component(
	property = "model.class.name=com.liferay.sync.model.SyncDevice",
	service = AopService.class
)
public class SyncDeviceLocalServiceImpl extends SyncDeviceLocalServiceBaseImpl {

	@Override
	public SyncDevice addSyncDevice(
			long userId, String type, long buildNumber, String hostname,
			int featureSet)
		throws PortalException {

		User user = userLocalService.getUser(userId);
		Date date = new Date();

		long syncDeviceId = counterLocalService.increment();

		SyncDevice syncDevice = syncDevicePersistence.create(syncDeviceId);

		syncDevice.setCompanyId(user.getCompanyId());
		syncDevice.setUserId(user.getUserId());
		syncDevice.setUserName(user.getFullName());
		syncDevice.setCreateDate(date);
		syncDevice.setModifiedDate(date);
		syncDevice.setType(type);
		syncDevice.setBuildNumber(buildNumber);
		syncDevice.setFeatureSet(featureSet);
		syncDevice.setHostname(hostname);
		syncDevice.setStatus(SyncDeviceConstants.STATUS_ACTIVE);

		return syncDevicePersistence.update(syncDevice);
	}

	@Override
	public List<SyncDevice> getSyncDevices(
			long userId, int start, int end,
			OrderByComparator<SyncDevice> orderByComparator)
		throws PortalException {

		return syncDevicePersistence.findByUserId(
			userId, start, end, orderByComparator);
	}

	@Override
	public List<SyncDevice> search(
		long companyId, String keywords, int start, int end,
		OrderByComparator<SyncDevice> orderByComparator) {

		return syncDevicePersistence.findByC_U(
			companyId, StringUtil.quote(keywords, StringPool.PERCENT), start,
			end, orderByComparator);
	}

	@Override
	public void updateStatus(long syncDeviceId, int status)
		throws PortalException {

		SyncDevice syncDevice = syncDevicePersistence.findByPrimaryKey(
			syncDeviceId);

		syncDevice.setStatus(status);

		syncDevicePersistence.update(syncDevice);
	}

	@Override
	public SyncDevice updateSyncDevice(
			long syncDeviceId, String type, long buildNumber, int featureSet,
			String hostname, int status)
		throws PortalException {

		SyncDevice syncDevice = syncDevicePersistence.findByPrimaryKey(
			syncDeviceId);

		syncDevice.setModifiedDate(new Date());
		syncDevice.setType(type);
		syncDevice.setBuildNumber(buildNumber);
		syncDevice.setFeatureSet(featureSet);
		syncDevice.setHostname(hostname);
		syncDevice.setStatus(status);

		return syncDevicePersistence.update(syncDevice);
	}

}