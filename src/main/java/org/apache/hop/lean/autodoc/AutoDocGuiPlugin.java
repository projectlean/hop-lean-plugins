package org.apache.hop.lean.autodoc;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.gui.plugin.GuiPlugin;
import org.apache.hop.core.gui.plugin.menu.GuiMenuElement;
import org.apache.hop.core.gui.plugin.toolbar.GuiToolbarElement;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.vfs.HopVfs;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.metadata.api.IHopMetadataSerializer;
import org.apache.hop.ui.core.dialog.EnterSelectionDialog;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.hopgui.HopGui;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.lean.core.LeanAttachment;
import org.lean.core.LeanColumn;
import org.lean.core.LeanFont;
import org.lean.core.LeanSortMethod;
import org.lean.presentation.LeanPresentation;
import org.lean.presentation.component.LeanComponent;
import org.lean.presentation.component.pipeline.LeanPipelineComponent;
import org.lean.presentation.component.types.composite.LeanCompositeComponent;
import org.lean.presentation.component.types.group.LeanGroupComponent;
import org.lean.presentation.component.types.label.LeanLabelComponent;
import org.lean.presentation.component.types.svg.LeanSvgComponent;
import org.lean.presentation.component.types.svg.ScaleType;
import org.lean.presentation.component.workflow.pipeline.LeanWorkflowComponent;
import org.lean.presentation.connector.LeanConnector;
import org.lean.presentation.connector.types.list.LeanListConnector;
import org.lean.presentation.layout.LeanLayout;
import org.lean.presentation.layout.LeanLayoutBuilder;
import org.lean.presentation.layout.LeanLayoutResults;
import org.lean.presentation.page.LeanPage;
import org.lean.presentation.theme.LeanTheme;
import org.lean.render.IRenderContext;
import org.lean.render.context.SimpleRenderContext;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@GuiPlugin
public class AutoDocGuiPlugin {

  public static final String ID_MAIN_MENU_FILE_NEW = "10060-menu-file-generate-documentation";
  public static final String ID_MAIN_TOOLBAR_GENERATE_DOCUMENTATION =
      "toolbar-10055-generate-documentation";

  public static final String CONNECTOR_NAME_WORKFLOW_FILENAMES = "workflow-filenames";
  public static final String CONNECTOR_NAME_PIPELINE_FILENAMES = "pipeline-filenames";
  public static final String FIELD_NAME_FILENAME = "filename";
  public static final String COMPONENT_NAME_WORKFLOWS = "workflows";
  public static final String COMPONENT_NAME_WORKFLOW_COMPOSITE = "workflow-composite";
  public static final String COMPONENT_NAME_WORKFLOW_FILENAME = "workflow-filename";
  public static final String COMPONENT_NAME_WORKFLOW_DESCRIPTION = "workflow-description";
  public static final String COMPONENT_NAME_WORKFLOW_CHANGED_DATE = "workflow-changed-date";
  public static final String COMPONENT_NAME_WORKFLOW_IMAGE = "workflow-image";
  public static final String COMPONENT_NAME_WORKFLOWS_GROUP = "workflows-group";

  public static final String COMPONENT_NAME_PIPELINES = "pipelines";
  public static final String COMPONENT_NAME_PIPELINE_COMPOSITE = "pipeline-composite";
  public static final String COMPONENT_NAME_PIPELINE_FILENAME = "pipeline-filename";
  public static final String COMPONENT_NAME_PIPELINE_DESCRIPTION = "workflow-description";
  public static final String COMPONENT_NAME_PIPELINE_CHANGED_DATE = "workflow-changed-date";
  public static final String COMPONENT_NAME_PIPELINE_IMAGE = "pipeline-image";
  public static final String COMPONENT_NAME_PIPELINES_GROUP = "pipelines-group";

  public static final String VARIABLE_NAME_NAME = "name";
  public static final String VARIABLE_NAME_DESCRIPTION = "description";
  public static final String VARIABLE_NAME_CHANGED_DATE = "changed_date";

  public static final int X_MARGIN = 25;
  public static final int Y_MARGIN = 10;

  public AutoDocGuiPlugin() {}

  @GuiMenuElement(
      root = HopGui.ID_MAIN_MENU,
      id = ID_MAIN_MENU_FILE_NEW,
      label = "i18n::AutoDocGuiPlugin.Menu.File.GenerateDocumentation",
      image = "book.svg",
      parentId = HopGui.ID_MAIN_MENU_FILE)
  @GuiToolbarElement(
      root = HopGui.ID_MAIN_TOOLBAR,
      id = ID_MAIN_TOOLBAR_GENERATE_DOCUMENTATION,
      image = "book.svg",
      toolTip = "i18n::AutoDocGuiPlugin.Toolbar.Item.GenerateDocumentation",
      separator = true)
  public void generateDocumentation() {
    HopGui hopGui = HopGui.getInstance();
    Shell shell = hopGui.getShell();
    IVariables variables = hopGui.getVariables();
    IHopMetadataProvider metadataProvider = hopGui.getMetadataProvider();

    try {
      // Ask for the metadata definition to use...
      //
      IHopMetadataSerializer<HopDocumentation> serializer =
          metadataProvider.getSerializer(HopDocumentation.class);

      List<String> names = serializer.listObjectNames();
      if (names.isEmpty()) {
        MessageBox messageBox = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
        messageBox.setText("Create documentation metadata");
        messageBox.setMessage(
            "Please create one or more Hop Documentation metadata objects to select from");
        messageBox.open();
        return;
      }

      EnterSelectionDialog enterSelectionDialog =
          new EnterSelectionDialog(
              shell,
              names.toArray(new String[0]),
              "Select documentation set",
              "Select the documentation metadata to use");
      String metadataName = enterSelectionDialog.open();
      if (metadataName == null) {
        return;
      }

      HopDocumentation hopDoc = serializer.load(metadataName);
      generateDocumentation(hopGui, variables, metadataProvider, hopDoc);
    } catch (Exception e) {
      new ErrorDialog(shell, "Error", "There was an error generating documentation", e);
    }
  }

  public void generateDocumentation(
      HopGui hopGui,
      IVariables variables,
      IHopMetadataProvider metadataProvider,
      HopDocumentation hopDoc)
      throws Exception {
    FileObject baseFolder = HopVfs.getFileObject(variables.resolve(hopDoc.getSourceFolder()));
    String targetFolderName = variables.resolve(hopDoc.getTargetFolder());
    String baseFilename = variables.resolve(hopDoc.getBaseFilename());
    FileObject targetFolder = HopVfs.getFileObject(targetFolderName);

    // Some sanity check before we do all the hard work...
    //
    if (!targetFolder.exists()) {
      if (!hopDoc.isCreateTargetFolder()) {
        throw new HopException(
            "Documentation target folder '" + targetFolderName + " doesn't exist");
      }
    }

    String presentationName = "Apache Hop documentation";
    String projectName = variables.getVariable("HOP_PROJECT_NAME");
    if (StringUtils.isNotEmpty(projectName)) {
      presentationName += " for project " + projectName;
    }

    // Start with an empty presentation...
    //
    LeanPresentation presentation = new LeanPresentation();
    presentation.setName(presentationName);
    presentation.setDescription("This presentation was automatically generated by Lean");

    // Add a default theme scheme.
    // It has information about background colors, chart colors and so on.
    //
    LeanTheme theme = LeanTheme.getDefault();
    presentation.getThemes().add(theme);

    // Setting a default theme allows all components in the presentation to use it
    //
    presentation.setDefaultThemeName(theme.getName());

    // Add a header and a footer
    //
    addHeaderFooter(presentation, presentationName, variables);
    if (hopDoc.isDocumentWorkflows()) {
      addWorkflows(presentation, variables, metadataProvider, baseFolder);
    }
    if (hopDoc.isDocumentPipelines()) {
      addPipelines(presentation, variables, metadataProvider, baseFolder);
    }

    // TODO: use the page format
    //
    LeanPage a4 = LeanPage.getA4(1, hopDoc.isPortrait());
    IRenderContext renderContext =
        new SimpleRenderContext(a4.getWidth(), a4.getHeight(), presentation.getThemes());
    LeanLayoutResults results =
        presentation.doLayout(
            hopGui.getLoggingObject(), renderContext, metadataProvider, new ArrayList<>());
    presentation.render(results, metadataProvider);

    // Convert to a PDF...
    // Target folder(s) get created automatically below
    //
    results.saveSvgPages(targetFolderName, baseFilename, true, true, true);

    // Save the presentation JSON...
    //
    String json = presentation.toJsonString(true);
    try (FileOutputStream fos =
        new FileOutputStream(targetFolderName + "/" + baseFilename + "-presentation.json")) {
      fos.write(json.getBytes(StandardCharsets.UTF_8));
      fos.flush();
    }
  }

  private void addWorkflows(
      LeanPresentation presentation,
      IVariables variables,
      IHopMetadataProvider metadataProvider,
      FileObject baseFolder)
      throws Exception {
    // One virtual page
    //
    LeanPage page = LeanPage.getA4(1, true);
    presentation.getPages().add(page);

    // A label at the top: Workflows
    //
    LeanLabelComponent headerLabelComponent = new LeanLabelComponent();
    headerLabelComponent.setLabel("Workflows");
    headerLabelComponent.setUnderline(true);
    headerLabelComponent.setDefaultFont(new LeanFont("Arial", "14", true, true));
    LeanComponent headerLabel = new LeanComponent(COMPONENT_NAME_WORKFLOWS, headerLabelComponent);
    headerLabel.setLayout(LeanLayout.topLeftPage());
    page.getComponents().add(headerLabel);

    // Find the workflows
    final List<String> filenames = findFiles(baseFolder, "hwf");

    // Create a connector for this list of filenames...
    //
    LeanListConnector leanFilenamesConnector =
        new LeanListConnector(FIELD_NAME_FILENAME, filenames);
    LeanConnector filenamesConnector =
        new LeanConnector(CONNECTOR_NAME_WORKFLOW_FILENAMES, leanFilenamesConnector);
    presentation.getConnectors().add(filenamesConnector);

    /**
     * The hierarchy below is that the Group loops over all the workflow filenames. This is
     * workflowGroup. We don't loop over a single component but a composite called
     * workflowCompositeComponent. Each composite contains labels and the image
     *
     * <p>Workflow: - Filename - Description: Image
     */
    LeanCompositeComponent workflowCompositeComponent = new LeanCompositeComponent();
    LeanComponent workflowComposite =
        new LeanComponent(COMPONENT_NAME_WORKFLOW_COMPOSITE, workflowCompositeComponent);

    // Add a listener to the composite to set a few extra variables...
    // - Description, Name, last changed date...
    //
    workflowComposite
        .getProcessSourceDataListeners()
        .add(new WorkflowProcessSourceDataListener(baseFolder, metadataProvider));

    // Filename
    {
      LeanLabelComponent labelComponent = new LeanLabelComponent();
      labelComponent.setLabel("Filename:      ${" + FIELD_NAME_FILENAME + "}");
      LeanComponent component = new LeanComponent(COMPONENT_NAME_WORKFLOW_FILENAME, labelComponent);
      component.setLayout(new LeanLayoutBuilder().top(Y_MARGIN).left().build());
      workflowCompositeComponent.getChildren().add(component);
    }
    String lastComponent = COMPONENT_NAME_WORKFLOW_FILENAME;

    // Description
    {
      LeanLabelComponent labelComponent = new LeanLabelComponent();
      labelComponent.setLabel("Description: ${" + VARIABLE_NAME_DESCRIPTION + "}");
      LeanComponent component =
          new LeanComponent(COMPONENT_NAME_WORKFLOW_DESCRIPTION, labelComponent);
      LeanLayout layout = new LeanLayoutBuilder().below(lastComponent, Y_MARGIN).build();
      component.setLayout(layout);
      workflowCompositeComponent.getChildren().add(component);
      lastComponent = component.getName();
    }

    // Changed date
    {
      LeanLabelComponent labelComponent = new LeanLabelComponent();
      labelComponent.setLabel("Changed:      ${" + VARIABLE_NAME_CHANGED_DATE + "}");
      LeanComponent component =
          new LeanComponent(COMPONENT_NAME_WORKFLOW_CHANGED_DATE, labelComponent);
      LeanLayout layout = new LeanLayoutBuilder().below(lastComponent, Y_MARGIN).build();
      component.setLayout(layout);
      workflowCompositeComponent.getChildren().add(component);
      lastComponent = component.getName();
    }

    // The image of the workflow
    {
      LeanWorkflowComponent imageComponent =
          new LeanWorkflowComponent(baseFolder + "/${" + FIELD_NAME_FILENAME + "}");
      LeanComponent component = new LeanComponent(COMPONENT_NAME_WORKFLOW_IMAGE, imageComponent);
      LeanLayout layout = new LeanLayoutBuilder().below(lastComponent, Y_MARGIN).right(0, -X_MARGIN).build();
      layout.getLeft().setOffset(25);
      component.setLayout(layout);
      workflowCompositeComponent.getChildren().add(component);
    }

    // Add the workflows group which loops over the composites
    //
    LeanGroupComponent workflowGroupComponent =
        new LeanGroupComponent(
            CONNECTOR_NAME_WORKFLOW_FILENAMES,
            Collections.singletonList(new LeanColumn(FIELD_NAME_FILENAME)),
            Collections.singletonList(new LeanSortMethod(LeanSortMethod.Type.STRING_ALPHA, true)),
            false,
            workflowComposite,
            10 // vertical margin
            );
    LeanComponent workflowGroup =
        new LeanComponent(COMPONENT_NAME_WORKFLOWS_GROUP, workflowGroupComponent);
    workflowGroup.setLayout(
        new LeanLayoutBuilder().below(COMPONENT_NAME_WORKFLOWS, Y_MARGIN).right().build());
    page.getComponents().add(workflowGroup);
  }

  private List<String> findFiles(FileObject baseFolder, String extension) throws Exception {
    List<FileObject> workflowFiles = HopVfs.findFiles(baseFolder, extension, true);
    final List<String> filenames = new ArrayList<>();
    workflowFiles.forEach(
        fo -> {
          filenames.add(fo.toString().replace( baseFolder + "/", ""));
        });
    return filenames;
  }

  private void addPipelines(
      LeanPresentation presentation,
      IVariables variables,
      IHopMetadataProvider metadataProvider,
      FileObject baseFolder)
      throws Exception {
    // One virtual page
    //
    LeanPage page = LeanPage.getA4(2, true);
    presentation.getPages().add(page);

    // A label at the top: Pipelines
    //
    {
      LeanLabelComponent headerLabelComponent = new LeanLabelComponent();
      headerLabelComponent.setLabel("Pipelines");
      headerLabelComponent.setUnderline(true);
      headerLabelComponent.setDefaultFont(new LeanFont("Arial", "14", true, true));
      LeanComponent headerLabel = new LeanComponent(COMPONENT_NAME_PIPELINES, headerLabelComponent);
      headerLabel.setLayout(LeanLayout.topLeftPage());
      page.getComponents().add(headerLabel);
    }

    // Find the workflows
    final List<String> filenames = findFiles(baseFolder, "hpl");

    // Create a connector for this list of filenames...
    //
    LeanListConnector leanFilenamesConnector =
        new LeanListConnector(FIELD_NAME_FILENAME, filenames);
    LeanConnector filenamesConnector =
        new LeanConnector(CONNECTOR_NAME_PIPELINE_FILENAMES, leanFilenamesConnector);
    presentation.getConnectors().add(filenamesConnector);

    /**
     * The hierarchy below is that the Group loops over all the pipeline filenames. This is
     * pipelineGroup. We don't loop over a single component but a composite called
     * pipelineCompositeComponent. Each composite contains labels and the image
     *
     * <p>Pipeline: - Filename - Description: Image
     */
    LeanCompositeComponent pipelineCompositeComponent = new LeanCompositeComponent();

    LeanComponent pipelineComposite =
        new LeanComponent(COMPONENT_NAME_PIPELINE_COMPOSITE, pipelineCompositeComponent);

    // Add a listener to the composite to set a few extra variables...
    // - Description, Name, last changed date...
    //
    pipelineComposite
        .getProcessSourceDataListeners()
        .add(new PipelineProcessSourceDataListener(baseFolder, metadataProvider));

    // Filename
    {
      LeanLabelComponent labelComponent = new LeanLabelComponent();
      labelComponent.setLabel("Filename:      ${" + FIELD_NAME_FILENAME + "}");
      LeanComponent component = new LeanComponent(COMPONENT_NAME_PIPELINE_FILENAME, labelComponent);
      component.setLayout(new LeanLayoutBuilder().top(Y_MARGIN).left().build());
      pipelineCompositeComponent.getChildren().add(component);
    }
    String lastComponent = COMPONENT_NAME_PIPELINE_FILENAME;

    // Description
    {
      LeanLabelComponent labelComponent = new LeanLabelComponent();
      labelComponent.setLabel("Description: ${" + VARIABLE_NAME_DESCRIPTION + "}");
      LeanComponent component =
          new LeanComponent(COMPONENT_NAME_PIPELINE_DESCRIPTION, labelComponent);
      LeanLayout layout = new LeanLayoutBuilder().below(lastComponent, Y_MARGIN).build();
      component.setLayout(layout);
      pipelineCompositeComponent.getChildren().add(component);
      lastComponent = component.getName();
    }

    // Changed date
    {
      LeanLabelComponent labelComponent = new LeanLabelComponent();
      labelComponent.setLabel("Changed:      ${" + VARIABLE_NAME_CHANGED_DATE + "}");
      LeanComponent component =
          new LeanComponent(COMPONENT_NAME_PIPELINE_CHANGED_DATE, labelComponent);
      LeanLayout layout = new LeanLayoutBuilder().below(lastComponent, Y_MARGIN).build();
      component.setLayout(layout);
      pipelineCompositeComponent.getChildren().add(component);
      lastComponent = component.getName();
    }

    // The image of the pipeline
    {
      LeanPipelineComponent imageComponent =
          new LeanPipelineComponent(baseFolder + "/${" + FIELD_NAME_FILENAME + "}");
      LeanComponent component = new LeanComponent(COMPONENT_NAME_PIPELINE_IMAGE, imageComponent);
      LeanLayout layout = LeanLayout.under(lastComponent, false);
      layout.getTop().setOffset(15);
      layout.getLeft().setOffset(25);
      layout.setRight(new LeanAttachment(null, 0, -X_MARGIN, LeanAttachment.Alignment.RIGHT));
      component.setLayout(layout);
      pipelineCompositeComponent.getChildren().add(component);
    }

    // Add the pipelines group which loops over the composites
    //
    LeanGroupComponent pipelineGroupComponent =
        new LeanGroupComponent(
            CONNECTOR_NAME_PIPELINE_FILENAMES,
            Collections.singletonList(new LeanColumn(FIELD_NAME_FILENAME)),
            Collections.singletonList(new LeanSortMethod(LeanSortMethod.Type.STRING_ALPHA, true)),
            false,
            pipelineComposite,
            10 // vertical margin
            );
    LeanComponent pipelineGroup =
        new LeanComponent(COMPONENT_NAME_PIPELINES_GROUP, pipelineGroupComponent);
    pipelineGroup.setLayout(
        new LeanLayoutBuilder().below(COMPONENT_NAME_PIPELINES, Y_MARGIN).right().build());
    page.getComponents().add(pipelineGroup);
  }

  private void addHeaderFooter(
      LeanPresentation presentation, String headerTitle, IVariables variables) {
    // Header
    LeanPage headerPage = LeanPage.getHeaderFooter(true, true, 25);
    headerPage.getComponents().add(createHeaderLabelComponent(headerTitle));
    presentation.setHeader(headerPage);
    // Footer
    LeanPage footer = LeanPage.getHeaderFooter(false, true, 25);
    footer.getComponents().add(createPageNumberLabelComponent());
    footer.getComponents().add(createSysdateLabelComponent());
    footer.getComponents().add(createFooterImageComponent());
    presentation.setFooter(footer);
  }

  protected static LeanComponent createFooterImageComponent() {
    LeanSvgComponent leanSvg = new LeanSvgComponent("hop-logo.svg", ScaleType.MIN);
    LeanComponent imageComponent = new LeanComponent("header-hop-logo", leanSvg);
    imageComponent.setLayout(new LeanLayoutBuilder().left(50,-10).top().bottom().build());

    return imageComponent;
  }

  protected static LeanComponent createHeaderLabelComponent(String headerMessage) {
    LeanLabelComponent label = new LeanLabelComponent();
    label.setLabel(headerMessage);
    LeanComponent labelComponent = new LeanComponent("header-message-label", label);
    labelComponent.setLayout(new LeanLayoutBuilder().leftCenter().top().build());
    return labelComponent;
  }

  protected static LeanComponent createPageNumberLabelComponent() {
    LeanLabelComponent label = new LeanLabelComponent();
    label.setLabel("Page #${PAGE_NUMBER}");
    LeanComponent labelComponent = new LeanComponent("footer-page-number-label", label);
    labelComponent.setLayout(new LeanLayoutBuilder().left().bottom().build());
    return labelComponent;
  }

  protected static LeanComponent createSysdateLabelComponent() {
    LeanLabelComponent label = new LeanLabelComponent();
    label.setLabel("${SYSTEM_DATE}");
    LeanComponent labelComponent = new LeanComponent("footer-system-date-label", label);
    labelComponent.setLayout(new LeanLayoutBuilder().right().bottom().build());
    return labelComponent;
  }
}
