/*
 * Copyright 2021-2023 VMware, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

import { Injectable } from '@angular/core';

import { AppConfig } from './app-config.model';

@Injectable()
export class AppConfigService {
    private static appConfig: AppConfig;
    private static jsonURL: string;

    constructor() {}

    async loadConfig(url: string): Promise<boolean> {
        AppConfigService.jsonURL = url;
        try {
            const response = await fetch(AppConfigService.jsonURL);
            const body = await response.json();
            AppConfigService.appConfig = JSON.parse(body) as AppConfig;
            return true;
        } catch (err) {
            console.error('Environment variable file was not found, application will not load');
            console.error(err);
            return false;
        }
    }

    public static get config(): AppConfig {
        return AppConfigService.appConfig;
    }

    public static get jsonUrl(): string {
        return AppConfigService.jsonURL;
    }
}
