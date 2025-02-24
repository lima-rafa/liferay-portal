/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 *
 *
 *
 */

package com.liferay.commerce.organization.web.internal.pagination;

import com.liferay.frontend.taglib.clay.data.Pagination;

/**
 * @author Marco Leo
 */
public class PaginationImpl implements Pagination {

	public PaginationImpl(int pageSize, int page) {
		_pageSize = pageSize;
		_page = page;
	}

	@Override
	public int getEndPosition() {
		return _page * _pageSize;
	}

	@Override
	public int getPage() {
		return _page;
	}

	@Override
	public int getPageSize() {
		return _pageSize;
	}

	@Override
	public int getStartPosition() {
		return (_page - 1) * _pageSize;
	}

	private final int _page;
	private final int _pageSize;

}