import {
  JupyterFrontEnd,
  JupyterFrontEndPlugin
} from '@jupyterlab/application';

import { ISettingRegistry } from '@jupyterlab/settingregistry';
import { updateVDKMenu } from './commandsAndMenu';


/**
 * Initialization data for the vdk-jupyterlab-extension extension.
 */
const plugin: JupyterFrontEndPlugin<void> = {
  id: 'vdk-jupyterlab-extension:plugin',
  autoStart: true,
  optional: [ISettingRegistry],
  activate: (app: JupyterFrontEnd, settingRegistry: ISettingRegistry | null) => {
    console.log('JupyterLab extension vdk-jupyterlab-extension is activated!');
    const { commands } = app;
    updateVDKMenu(commands);
  }
};

export default plugin;
