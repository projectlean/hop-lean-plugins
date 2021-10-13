package org.apache.hop.lean.autodoc;

import org.apache.hop.core.gui.plugin.GuiElementType;
import org.apache.hop.core.gui.plugin.GuiPlugin;
import org.apache.hop.core.gui.plugin.GuiWidgetElement;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.metadata.api.HopMetadata;
import org.apache.hop.metadata.api.HopMetadataBase;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadata;
import org.apache.hop.metadata.api.IHopMetadataProvider;

import java.util.Arrays;
import java.util.List;

@HopMetadata(
    key = "documentation",
    name = "Documentation",
    description = "Defines a documentation set which can be generated",
    image = "book.svg")
@GuiPlugin
public class HopDocumentation extends HopMetadataBase implements IHopMetadata {

  public static final String GUI_PLUGIN_ELEMENT_PARENT_ID = "GuiPlugin-AutoDoc-Parent";

  @GuiWidgetElement(
      order = "10000-source-folder",
      parentId = GUI_PLUGIN_ELEMENT_PARENT_ID,
      type = GuiElementType.TEXT,
      label = "Documentation source folder",
      toolTip = "This is the base folder to document")
  @HopMetadataProperty
  private String sourceFolder;

  @GuiWidgetElement(
      order = "10100-target-folder",
      parentId = GUI_PLUGIN_ELEMENT_PARENT_ID,
      type = GuiElementType.TEXT,
      label = "Documentation target folder",
      toolTip = "Intermediary files as well as results will be saved here")
  @HopMetadataProperty
  private String targetFolder;

  @GuiWidgetElement(
      order = "10200-create-target-folder",
      parentId = GUI_PLUGIN_ELEMENT_PARENT_ID,
      type = GuiElementType.CHECKBOX,
      label = "Create target folder?",
      toolTip = "Create the specified target folder(s) if it doesn't exist")
  @HopMetadataProperty
  private boolean createTargetFolder;

  @GuiWidgetElement(
    order = "10250-base-filename",
    parentId = GUI_PLUGIN_ELEMENT_PARENT_ID,
    type = GuiElementType.TEXT,
    label = "Base filename",
    toolTip = "The base filename")
  @HopMetadataProperty
  private String baseFilename;

  @GuiWidgetElement(
      order = "10300-search-subfolders",
      parentId = GUI_PLUGIN_ELEMENT_PARENT_ID,
      type = GuiElementType.CHECKBOX,
      label = "Search sub-folders?",
      toolTip = "Search the source folder and sub-folders")
  @HopMetadataProperty
  private boolean searchSubFolders;

  @GuiWidgetElement(
      order = "10400-document-workflows",
      parentId = GUI_PLUGIN_ELEMENT_PARENT_ID,
      type = GuiElementType.CHECKBOX,
      label = "Document workflows?",
      toolTip = "Documents all the workflows found in the source folder")
  @HopMetadataProperty
  private boolean documentWorkflows;

  @GuiWidgetElement(
      order = "10500-document-pipelines",
      parentId = GUI_PLUGIN_ELEMENT_PARENT_ID,
      type = GuiElementType.CHECKBOX,
      label = "Document pipelines?",
      toolTip = "Documents all the pipelines found in the source folder")
  @HopMetadataProperty
  private boolean documentPipelines;

  @GuiWidgetElement(
      order = "10600-portrait",
      parentId = GUI_PLUGIN_ELEMENT_PARENT_ID,
      type = GuiElementType.CHECKBOX,
      label = "Portrait format?",
      toolTip = "Disable for landscape, enable for portrait")
  @HopMetadataProperty
  private boolean portrait;

  @GuiWidgetElement(
      order = "10700-page-format",
      parentId = GUI_PLUGIN_ELEMENT_PARENT_ID,
      type = GuiElementType.COMBO,
      label = "Page format",
      toolTip = "The page format (not yet supported)",
      comboValuesMethod = "getPaperFormats")
  @HopMetadataProperty
  private String pageFormat;

  public HopDocumentation() {
    this("documentation");
  }

  public HopDocumentation(String name) {
    super(name);
    this.sourceFolder = "${PROJECT_HOME}";
    this.targetFolder = "${java.io.tmpdir}/autodoc";
    this.createTargetFolder = true;
    this.baseFilename = "autodoc";
    this.searchSubFolders = true;
    this.documentWorkflows = true;
    this.documentPipelines = true;
    this.portrait = true;
    this.pageFormat = "A4";
  }

  public HopDocumentation( HopDocumentation d ) {
    super( d.name );
    this.sourceFolder = d.sourceFolder;
    this.targetFolder = d.targetFolder;
    this.createTargetFolder = d.createTargetFolder;
    this.baseFilename = d.baseFilename;
    this.searchSubFolders = d.searchSubFolders;
    this.documentWorkflows = d.documentWorkflows;
    this.documentPipelines = d.documentPipelines;
    this.portrait = d.portrait;
    this.pageFormat = d.pageFormat;
  }

  @Override protected HopDocumentation clone() {
    return new HopDocumentation(this);
  }

  public List<String> getPaperFormats( ILogChannel log, IHopMetadataProvider metadataProvider) {
    return Arrays.asList( "A4" );
  }

  /**
   * Gets sourceFolder
   *
   * @return value of sourceFolder
   */
  public String getSourceFolder() {
    return sourceFolder;
  }

  /** @param sourceFolder The sourceFolder to set */
  public void setSourceFolder(String sourceFolder) {
    this.sourceFolder = sourceFolder;
  }

  /**
   * Gets targetFolder
   *
   * @return value of targetFolder
   */
  public String getTargetFolder() {
    return targetFolder;
  }

  /** @param targetFolder The targetFolder to set */
  public void setTargetFolder(String targetFolder) {
    this.targetFolder = targetFolder;
  }

  /**
   * Gets documentWorkflows
   *
   * @return value of documentWorkflows
   */
  public boolean isDocumentWorkflows() {
    return documentWorkflows;
  }

  /** @param documentWorkflows The documentWorkflows to set */
  public void setDocumentWorkflows(boolean documentWorkflows) {
    this.documentWorkflows = documentWorkflows;
  }

  /**
   * Gets documentPipelines
   *
   * @return value of documentPipelines
   */
  public boolean isDocumentPipelines() {
    return documentPipelines;
  }

  /** @param documentPipelines The documentPipelines to set */
  public void setDocumentPipelines(boolean documentPipelines) {
    this.documentPipelines = documentPipelines;
  }

  /**
   * Gets portrait
   *
   * @return value of portrait
   */
  public boolean isPortrait() {
    return portrait;
  }

  /** @param portrait The portrait to set */
  public void setPortrait(boolean portrait) {
    this.portrait = portrait;
  }

  /**
   * Gets pageFormat
   *
   * @return value of pageFormat
   */
  public String getPageFormat() {
    return pageFormat;
  }

  /** @param pageFormat The pageFormat to set */
  public void setPageFormat(String pageFormat) {
    this.pageFormat = pageFormat;
  }

  /**
   * Gets createTargetFolder
   *
   * @return value of createTargetFolder
   */
  public boolean isCreateTargetFolder() {
    return createTargetFolder;
  }

  /**
   * @param createTargetFolder The createTargetFolder to set
   */
  public void setCreateTargetFolder( boolean createTargetFolder ) {
    this.createTargetFolder = createTargetFolder;
  }

  /**
   * Gets baseFilename
   *
   * @return value of baseFilename
   */
  public String getBaseFilename() {
    return baseFilename;
  }

  /**
   * @param baseFilename The baseFilename to set
   */
  public void setBaseFilename( String baseFilename ) {
    this.baseFilename = baseFilename;
  }

  /**
   * Gets searchSubFolders
   *
   * @return value of searchSubFolders
   */
  public boolean isSearchSubFolders() {
    return searchSubFolders;
  }

  /**
   * @param searchSubFolders The searchSubFolders to set
   */
  public void setSearchSubFolders( boolean searchSubFolders ) {
    this.searchSubFolders = searchSubFolders;
  }
}
