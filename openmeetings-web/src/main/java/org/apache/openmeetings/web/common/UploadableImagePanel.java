/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") +  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openmeetings.web.common;

import static org.apache.openmeetings.util.OpenmeetingsVariables.getMaxUploadSize;
import static org.apache.openmeetings.web.common.confirmation.ConfirmationBehavior.newOkCancelConfirm;

import java.io.File;
import java.util.Optional;

import org.apache.openmeetings.util.StoredFile;
import org.apache.openmeetings.web.util.upload.BootstrapFileUploadBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.extensions.ajax.markup.html.form.upload.UploadProgressBar;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.util.lang.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.core.markup.html.bootstrap.button.BootstrapAjaxLink;
import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;

public abstract class UploadableImagePanel extends ImagePanel {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(UploadableImagePanel.class);
	private final FileUploadField fileUploadField = new FileUploadField("image", new ListModel<>());
	private final Form<Void> form = new Form<>("form");
	private final boolean delayed;

	protected UploadableImagePanel(String id, boolean delayed) {
		super(id);
		this.delayed = delayed;
	}

	protected abstract void processImage(StoredFile sf, File f) throws Exception;

	protected abstract void deleteImage() throws Exception;

	@Override
	protected void onInitialize() {
		super.onInitialize();
		form.setMultiPart(true);
		form.setMaxSize(Bytes.bytes(getMaxUploadSize()));
		// Model is necessary here to avoid writing image to the User object
		form.add(fileUploadField);
		form.add(new UploadProgressBar("progress", form, fileUploadField));
		form.addOrReplace(getImage());
		if (delayed) {
			add(new WebMarkupContainer("remove"));
		} else {
			BootstrapAjaxLink<String> remove = new BootstrapAjaxLink<>("remove", Buttons.Type.Outline_Secondary) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target) {
					try {
						deleteImage();
					} catch (Exception e) {
						log.error("Error", e);
					}
					update(Optional.of(target));
				}
			};
			add(remove
					.setIconType(FontAwesome5IconType.times_s)
					.add(newOkCancelConfirm(this, getString("833"))));
			fileUploadField.add(new AjaxFormSubmitBehavior(form, "change") {
				private static final long serialVersionUID = 1L;

				@Override
				protected void onSubmit(AjaxRequestTarget target) {
					process(Optional.of(target));
				}
			});
		}
		add(form.setOutputMarkupId(true));
		add(BootstrapFileUploadBehavior.INSTANCE);
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		if (delayed) {
			response.render(OnDomReadyHeaderItem.forScript("$('#" + form.getOutputMarkupId() + " .remove').click(function() {$(this).parent().find('.fileinput').fileinput('clear');})"));
		}
	}

	@Override
	public void update() {
		profile.addOrReplace(new WebMarkupContainer("img").setVisible(false));
		form.addOrReplace(getImage());
	}

	private void update(Optional<AjaxRequestTarget> target) {
		update();
		target.ifPresent(t -> t.add(profile, form));
	}

	public void process(Optional<AjaxRequestTarget> target) {
		FileUpload fu = fileUploadField.getFileUpload();
		if (fu != null) {
			File temp = null;
			try {
				temp = fu.writeToTempFile();
				StoredFile sf = new StoredFile(fu.getClientFileName(), temp);
				if (sf.isImage()) {
					processImage(sf, temp);
				}
			} catch (Exception e) {
				log.error("Error", e);
			} finally {
				if (temp != null && temp.exists()) {
					log.debug("Temp file was deleted ? {}", temp.delete());
				}
				fu.closeStreams();
				fu.delete();
			}
		}
		update(target);
	}
}
