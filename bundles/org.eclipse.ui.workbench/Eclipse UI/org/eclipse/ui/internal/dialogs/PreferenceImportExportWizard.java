/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Common Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.ui.internal.dialogs;

import java.io.File;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.WorkbenchPlugin;

/**
 * The wizard responsible for handling both the import and export of
 * preferences to/from files. There is a lot of overlap in functionality, so
 * these wizards are simply implemented as one wizard with two modes.
 * 
 * @since 3.0
 */
public class PreferenceImportExportWizard extends Wizard {

	/**
	 * Whether this wizard should export. Once set, this value will not change
	 * during the life of the wizard.
	 */
	private final boolean export;
	/**
	 * The page containing the file selection controls. This is the first page
	 * shown to the user (and sometimes the only page). This value should not
	 * be <code>null</code> after the pages have been added.
	 */
	private PreferenceImportExportFileSelectionPage fileSelectionPage;
	/**
	 * The dialog which opened this wizard. This is used to get a handle on the
	 * preferences. This value should never be <code>null</code>.
	 */
	private final PreferenceDialog parent;
	/**
	 * The page containing the preferences selection controls. This is the
	 * second page shown to the user. This valud should not be <code>null</code>
	 * after the pages have been added.
	 */
	private PreferenceImportExportSettingsSelectionPage settingsSelectionPage;

	/**
	 * Constructs a new instance of <code>PreferenceImportExportWizard</code>
	 * with the mode and parent dialog.
	 * 
	 * @param exportWizard
	 *            Whether the wizard should act as an export tool.
	 * @param parentDialog
	 *            The dialog which created this wizard (<em>not</em> the
	 *            wizard dialog itself). This parameter should not be <code>null</code>.
	 */
	public PreferenceImportExportWizard(final boolean exportWizard,
			PreferenceDialog parentDialog) {
		super();
		export = exportWizard;
		parent = parentDialog;
		if (exportWizard) {
			setWindowTitle(WorkbenchMessages.getString("ImportExportPages.exportWindowTitle")); //$NON-NLS-1$
		} else {
			setWindowTitle(WorkbenchMessages.getString("ImportExportPages.importWindowTitle")); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	public void addPages() {
		super.addPages();
		fileSelectionPage = new PreferenceImportExportFileSelectionPage(export);
		addPage(fileSelectionPage);
		settingsSelectionPage = new PreferenceImportExportSettingsSelectionPage(export);
		addPage(settingsSelectionPage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#canFinish()
	 */
	public boolean canFinish() {
		if (fileSelectionPage.wantsPreferencesSelected()) {
			if (getContainer().getCurrentPage() == fileSelectionPage) {
				return false;
			}
			return fileSelectionPage.validate() && settingsSelectionPage.canFinish();
		}
		return fileSelectionPage.canFinish();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		// Save all the pages and give them a chance to abort
		Iterator nodes = parent.getPreferenceManager().getElements(PreferenceManager.PRE_ORDER)
				.iterator();
		while (nodes.hasNext()) {
			IPreferenceNode node = (IPreferenceNode) nodes.next();
			IPreferencePage page = node.getPage();
			if (page != null) {
				if (!page.performOk())
					return false;
			}
		}
		String selectedFilePath = fileSelectionPage.getPath();
		File selectedFile = new File(selectedFilePath);
		long lastModified = selectedFile.lastModified();

		// Save or load -- depending on the phase of moon.
		IPath path = new Path(selectedFilePath);
		if (export) {
			if (selectedFile.exists()) {
				if (!MessageDialog.openConfirm(
				    	getShell(), 
				    	WorkbenchMessages.getString("WorkbenchPreferenceDialog.saveTitle"), //$NON-NLS-1$
						WorkbenchMessages.format("WorkbenchPreferenceDialog.existsErrorMessage", //$NON-NLS-1$
						new Object[]{selectedFilePath})))
					return false;
			}
			
			try {
				Preferences.exportPreferences(path);
			} catch (CoreException e) {
				ErrorDialog.openError(
				    getShell(), 
				    WorkbenchMessages.getString("WorkbenchPreferenceDialog.saveErrorTitle"), //$NON-NLS-1$
					WorkbenchMessages.format("WorkbenchPreferenceDialog.saveErrorMessage", //$NON-NLS-1$
					new Object[]{selectedFilePath}),
					e.getStatus());
				return false;
			}
			
		} else {
			IStatus status = Preferences.validatePreferenceVersions(path);
			if (status.getSeverity() == IStatus.ERROR) {
				// Show the error and about
				ErrorDialog.openError(
				    getShell(), 
				    WorkbenchMessages.getString("WorkbenchPreferenceDialog.loadErrorTitle"), //$NON-NLS-1$
					WorkbenchMessages.format("WorkbenchPreferenceDialog.verifyErrorMessage", //$NON-NLS-1$
					new Object[]{selectedFilePath}),
					status);
				return false;
			} else if (status.getSeverity() == IStatus.WARNING) {
				// Show the warning and give the option to continue
				int result = PreferenceErrorDialog.openError(
				    getShell(), 
				    WorkbenchMessages.getString("WorkbenchPreferenceDialog.loadErrorTitle"), //$NON-NLS-1$
					WorkbenchMessages.format("WorkbenchPreferenceDialog.verifyWarningMessage", //$NON-NLS-1$
					new Object[]{selectedFilePath}),
					status);
				if (result != Window.OK) {
					return false;
				}
			}
			
			try {
				Preferences.importPreferences(path);
			} catch (CoreException e) {
				ErrorDialog.openError(
				    getShell(), 
				    WorkbenchMessages.getString("WorkbenchPreferenceDialog.loadErrorTitle"), //$NON-NLS-1$
					WorkbenchMessages.format("WorkbenchPreferenceDialog.loadErrorMessage", //$NON-NLS-1$
					new Object[]{selectedFilePath}),
					e.getStatus());
				return false;
			}
		}

		// See if we actually created a file (there where preferences to
		// export)
		if (!export) {
			MessageDialog.openInformation(
			    getShell(), 
			    WorkbenchMessages.getString("WorkbenchPreferenceDialog.loadTitle"), //$NON-NLS-1$
				WorkbenchMessages.format("WorkbenchPreferenceDialog.loadMessage", //$NON-NLS-1$
				new Object[]{selectedFilePath}));
			
		} else if ((selectedFile.exists() && (selectedFile.lastModified() != lastModified))) {
			MessageDialog.openInformation(
			    getShell(), 
			    WorkbenchMessages.getString("WorkbenchPreferenceDialog.saveTitle"), //$NON-NLS-1$
				WorkbenchMessages.format("WorkbenchPreferenceDialog.saveMessage", //$NON-NLS-1$
				new Object[]{selectedFilePath}));

		} else {
			MessageDialog.openError(
			    getShell(), 
			    WorkbenchMessages.getString("WorkbenchPreferenceDialog.saveErrorTitle"), //$NON-NLS-1$
				WorkbenchMessages.getString("WorkbenchPreferenceDialog.noPreferencesMessage")); //$NON-NLS-1$
		}

		// We have been successful!
		WorkbenchPlugin.getDefault().getDialogSettings().put(
			WorkbenchPreferenceDialog.FILE_PATH_SETTING, fileSelectionPage.getPath());
		return true;
	}
}
