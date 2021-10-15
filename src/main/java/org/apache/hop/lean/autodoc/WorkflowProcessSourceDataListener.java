package org.apache.hop.lean.autodoc;

import org.apache.commons.vfs2.FileObject;
import org.apache.hop.core.Const;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.vfs.HopVfs;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.workflow.WorkflowMeta;
import org.lean.core.exception.LeanException;
import org.lean.presentation.LeanPresentation;
import org.lean.presentation.component.LeanComponent;
import org.lean.presentation.component.listeners.IProcessSourceDataListener;
import org.lean.presentation.datacontext.IDataContext;
import org.lean.presentation.layout.LeanLayoutResults;
import org.lean.presentation.page.LeanPage;
import org.lean.render.IRenderContext;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WorkflowProcessSourceDataListener implements IProcessSourceDataListener {
  private FileObject baseFolder;
  private IHopMetadataProvider metadataProvider;

  public WorkflowProcessSourceDataListener(
    FileObject baseFolder, IHopMetadataProvider metadataProvider) {
    this.baseFolder = baseFolder;
    this.metadataProvider = metadataProvider;
  }

  @Override
  public void beforeProcessSourceDataCalled(
      LeanPresentation presentation,
      LeanPage page,
      LeanComponent component,
      IDataContext dataContext,
      IRenderContext renderContext,
      LeanLayoutResults results)
      throws LeanException {
    // Load the workflow metadata and set that information as variables
    // These can then be picked up by the various label components.
    //
    IVariables vars = dataContext.getVariables();
    String filename = baseFolder + "/" + vars.getVariable(AutoDocGuiPlugin.FIELD_NAME_FILENAME);
    try {
      // Load the workflow metadata
      //
      WorkflowMeta workflowMeta = new WorkflowMeta(vars, filename, metadataProvider);
      workflowMeta.setInternalHopVariables(vars);

      Date changedDate =
          new Date(HopVfs.getFileObject(filename).getContent().getLastModifiedTime());
      String changedDateString = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(changedDate);

      // Set the variables in the data context of the component
      //
      vars.setVariable(AutoDocGuiPlugin.VARIABLE_NAME_NAME, workflowMeta.getName());
      vars.setVariable(AutoDocGuiPlugin.VARIABLE_NAME_DESCRIPTION, Const.NVL(workflowMeta.getDescription(), ""));
      vars.setVariable(AutoDocGuiPlugin.VARIABLE_NAME_CHANGED_DATE, changedDateString);
    } catch (Exception e) {
      throw new LeanException(
          "Error loading workflow information from metadata for filename: " + filename, e);
    }
  }
}
