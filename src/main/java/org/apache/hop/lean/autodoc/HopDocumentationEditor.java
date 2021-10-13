package org.apache.hop.lean.autodoc;

import org.apache.hop.core.Const;
import org.apache.hop.core.gui.plugin.GuiPlugin;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.gui.GuiCompositeWidgets;
import org.apache.hop.ui.core.gui.GuiCompositeWidgetsAdapter;
import org.apache.hop.ui.core.metadata.MetadataEditor;
import org.apache.hop.ui.core.metadata.MetadataManager;
import org.apache.hop.ui.hopgui.HopGui;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

@GuiPlugin
public class HopDocumentationEditor extends MetadataEditor<HopDocumentation> {
  private static final Class<?> PKG = HopDocumentationEditor.class; // For Translator

  private final HopDocumentation hopDocumentation;
  private final HopDocumentation workingDocumentation;

  private Text wName;
  private Composite wPluginSpecificComp;
  private GuiCompositeWidgets guiCompositeWidgets;

  /**
   * @param hopGui
   * @param manager
   * @param hopDocumentation The object to edit
   */
  public HopDocumentationEditor(
      HopGui hopGui, MetadataManager<HopDocumentation> manager, HopDocumentation hopDocumentation) {
    super(hopGui, manager, hopDocumentation);

    this.hopDocumentation = hopDocumentation;
    this.workingDocumentation = new HopDocumentation(hopDocumentation);
  }

  @Override
  public void createControl(Composite parent) {
    PropsUi props = PropsUi.getInstance();

    // Create a tabbed interface instead of the confusing left hand side options
    // This will make it more conforming the rest.
    //
    int middle = props.getMiddlePct();
    int margin = props.getMargin();

    // The generic widgets: name, description and pipeline engine type
    //
    // What's the name
    //
    Label wlName = new Label(parent, SWT.RIGHT);
    props.setLook(wlName);
    wlName.setText(BaseMessages.getString(PKG, "HopDocumentationEditor.label.name"));
    FormData fdlName = new FormData();
    fdlName.top = new FormAttachment(0, 0);
    fdlName.left = new FormAttachment(0, 0); // First one in the left top corner
    fdlName.right = new FormAttachment(middle, 0);
    wlName.setLayoutData(fdlName);
    wName = new Text(parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    props.setLook(wName);
    FormData fdName = new FormData();
    fdName.top = new FormAttachment(wlName, 0, SWT.CENTER);
    fdName.left = new FormAttachment(middle, margin); // To the right of the label
    fdName.right = new FormAttachment(100, 0);
    wName.setLayoutData(fdName);
    Control lastControl = wName;

    // Add a composite area
    //
    wPluginSpecificComp = new Composite(parent, SWT.BACKGROUND);
    props.setLook(wPluginSpecificComp);
    wPluginSpecificComp.setLayout(new FormLayout());
    FormData fdPluginSpecificComp = new FormData();
    fdPluginSpecificComp.left = new FormAttachment(0, 0);
    fdPluginSpecificComp.right = new FormAttachment(100, 0);
    fdPluginSpecificComp.top = new FormAttachment(lastControl, 3 * margin);
    fdPluginSpecificComp.bottom = new FormAttachment(100, 0);
    wPluginSpecificComp.setLayoutData(fdPluginSpecificComp);

    // Add the plugin specific widgets
    //
    guiCompositeWidgets = new GuiCompositeWidgets(manager.getVariables());
    guiCompositeWidgets.createCompositeWidgets(
        hopDocumentation,
        null,
        wPluginSpecificComp,
        HopDocumentation.GUI_PLUGIN_ELEMENT_PARENT_ID,
        null);
    guiCompositeWidgets.setWidgetsListener(
        new GuiCompositeWidgetsAdapter() {
          @Override
          public void widgetModified(
              GuiCompositeWidgets compositeWidgets, Control changedWidget, String widgetId) {
            setChanged();
          }
        });

    setWidgetsContent();

    // Add changed listeners
    //
    wName.addListener(SWT.Modify, e -> setChanged());
    guiCompositeWidgets.setWidgetsListener(
        new GuiCompositeWidgetsAdapter() {
          @Override
          public void widgetModified(
              GuiCompositeWidgets compositeWidgets, Control changedWidget, String widgetId) {
            setChanged();
          }
        });
  }

  @Override
  public void setWidgetsContent() {
    HopDocumentation hopDocumentation = getMetadata();
    wName.setText(Const.NVL(hopDocumentation.getName(), ""));
    guiCompositeWidgets.setWidgetsContents(
        hopDocumentation, wPluginSpecificComp, HopDocumentation.GUI_PLUGIN_ELEMENT_PARENT_ID);
  }

  @Override
  public void getWidgetsContent(HopDocumentation hopDocumentation) {
    hopDocumentation.setName(wName.getText());
    guiCompositeWidgets.getWidgetsContents(
        hopDocumentation, HopDocumentation.GUI_PLUGIN_ELEMENT_PARENT_ID);
  }

  @Override
  public void setChanged() {
    resetChanged();
    super.setChanged();
  }

  @Override
  public Button[] createButtonsForButtonBar(Composite parent) {
    Button wbTest = new Button(parent, SWT.PUSH | SWT.CENTER);
    PropsUi.getInstance().setLook(wbTest);
    wbTest.setText("Test");
    wbTest.addListener(SWT.Selection, e -> generateDocumentation());
    return new Button[] {wbTest};
  }

  protected void generateDocumentation() {
    try {
      HopDocumentation hopDoc = new HopDocumentation();
      getWidgetsContent(hopDoc);

      new AutoDocGuiPlugin()
          .generateDocumentation(
              hopGui,
              manager.getVariables(),
              manager.getMetadataProvider(),
              hopDoc);
    } catch (Exception e) {
      new ErrorDialog(getShell(), "Error", "Error generating documentation", e);
    }
  }
}
