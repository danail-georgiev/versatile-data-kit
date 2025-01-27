import { CommandRegistry } from '@lumino/commands';
import { Dialog, showErrorMessage } from '@jupyterlab/apputils';
import { showRunJobDialog } from './components/RunJob';
import { jobData, setJobDataToDefault } from './jobData';
import { showCreateDeploymentDialog } from './components/DeployJob';
import { showCreateJobDialog } from './components/CreateJob';
import { showDownloadJobDialog } from './components/DownloadJob';
import { showConvertJobToNotebookDialog } from './components/ConvertJobToNotebook';
import { jobdDataRequest } from './serverRequests';
import { VdkOption } from './vdkOptions/vdk_options';
import { workingDirectory } from '.';
import { IDocumentManager } from '@jupyterlab/docmanager';

var runningVdkOperation = false;

export function updateVDKMenu(commands: CommandRegistry, docManager: IDocumentManager) {
  // Add Run job command
  add_command(commands, 'jp-vdk:menu-run','Run','Execute VDK Run Command', showRunJobDialog, docManager);

  // Add Create job command
  add_command(commands, 'jp-vdk:menu-create','Create','Execute VDK Create Command', showCreateJobDialog);

  // Add Download job command
  add_command(commands, 'jp-vdk:menu-download','Download','Execute VDK Download Command', showDownloadJobDialog);

  // Add Convert Job To Notebook command
  add_command(commands, 'jp-vdk:menu-convert-job-to-notebook', 'Convert Job To Notebook', 'Convert Data Job To Jupyter Notebook', showConvertJobToNotebookDialog);

  // Add Create Deployment command
  add_command(commands, 'jp-vdk:menu-create-deployment','Deploy','Create deployment of a VDK job', showCreateDeploymentDialog);
}


/**
 *@param schemaNaming - string representing the command in the schema in schema/plugin.json
 *@param label - the label that will be added in the Menu
 *@param caption - the caption for the command.
 *@param getOperationDialog - function that will load the dialog for the command
 */
function add_command(commands: CommandRegistry, schemaNaming: string, label: string, caption: string, getOperationDialog: Function,
  docManager?: IDocumentManager){
  commands.addCommand(schemaNaming, {
    label: label,
    caption: caption,
    execute: async () => {
      try {
        if (!runningVdkOperation) {
          runningVdkOperation = true;
          jobData.set(VdkOption.PATH, workingDirectory);
          await jobdDataRequest();
          if(docManager) await getOperationDialog(docManager);
          else  await getOperationDialog();
          setJobDataToDefault();
          runningVdkOperation = false;
        } else {
          showErrorMessage(
            'Another VDK operation is currently running!',
            'Please wait until the operation ends!',
            [Dialog.okButton()]
          );
        }
      } catch (error) {
        await showErrorMessage(
          'Encountered an error when trying to open the dialog. Error:',
          error,
          [Dialog.okButton()]
        );
      }
    }
  });
}
