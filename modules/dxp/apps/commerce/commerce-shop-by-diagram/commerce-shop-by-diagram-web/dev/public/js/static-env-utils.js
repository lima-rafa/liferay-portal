/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 */

const Liferay = {
	Language: {
		get: (key) => {
			let counter = 0;

			return key.replace(new RegExp('(^x-)|(-x-)|(-x$)', 'gm'), (match) =>
				match.replace('x', `{${counter++}}`)
			);
		},
	},
	ThemeDisplay: {
		getCanonicalURL: () => '/',
		getDefaultLanguageId: () => 'en_US',
		getLanguageId: () => 'it_IT',
		getPathThemeImages: () => '/assets',
		getPortalURL: () => window.location.origin,
		getScopeGroupId: () => '123',
	},
	Util: {
		sub: (key, ...values) => {
			return values.reduce(
				(acc, value, i) => acc.replace(new RegExp(`{[${i}]}`), value),
				key
			);
		},
	},
	component: () => {},
	detach: (name, fn) => {
		window.removeEventListener(name, fn);
	},
	fire: (name, payload) => {
		const event = document.createEvent('CustomEvent');
		event.initCustomEvent(name);
		if (payload) {
			Object.keys(payload).forEach((key) => {
				event[key] = payload[key];
			});
		}
		window.dispatchEvent(event);
	},
	on: (name, fn) => {
		window.addEventListener(name, fn);
	},
	staticEnvTestUtils: {
		print: (message, type) => ({message, type}),
	},
};

window.defaultFetch = fetch;

window.Liferay = Liferay;
window.themeDisplay = Liferay.ThemeDisplay;
