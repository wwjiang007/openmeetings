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
package org.apache.openmeetings.web.pages.auth;

import static org.apache.openmeetings.util.OpenmeetingsVariables.getBaseUrl;
import static org.apache.openmeetings.util.OpenmeetingsVariables.getMinLoginLength;
import static org.apache.openmeetings.util.OpenmeetingsVariables.isSendRegisterEmail;
import static org.apache.openmeetings.util.OpenmeetingsVariables.isSendVerificationEmail;
import static org.apache.wicket.validation.validator.StringValidator.minimumLength;

import org.apache.openmeetings.core.util.StrongPasswordValidator;
import org.apache.openmeetings.db.dao.user.IUserManager;
import org.apache.openmeetings.db.dao.user.UserDao;
import org.apache.openmeetings.db.entity.user.Address;
import org.apache.openmeetings.db.entity.user.User;
import org.apache.openmeetings.web.app.WebSession;
import org.apache.openmeetings.web.common.Captcha;
import org.apache.openmeetings.web.common.OmModalCloseButton;
import org.apache.openmeetings.web.pages.PrivacyPage;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.validation.validator.RfcCompliantEmailAddressValidator;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.IValidatable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.core.markup.html.bootstrap.common.NotificationPanel;
import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.spinner.SpinnerAjaxButton;

public class RegisterDialog extends Modal<String> {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(RegisterDialog.class);
	private final NotificationPanel feedback = new NotificationPanel("feedback");
	private final IModel<String> tzModel = Model.of(WebSession.get().getClientTZCode());
	private final RegisterForm form = new RegisterForm("form");
	private SignInDialog s;
	private Captcha captcha;
	private String firstName;
	private String lastName;
	private String login;
	private String password;
	private String email;
	private String country;
	private Long lang;
	private boolean wasRegistered = false;

	private final Modal<String> registerInfo;
	@SpringBean
	private IUserManager userManager;
	@SpringBean
	private UserDao userDao;

	public RegisterDialog(String id, Modal<String> registerInfo) {
		super(id);
		this.registerInfo = registerInfo;
	}

	@Override
	protected void onInitialize() {
		header(new ResourceModel("113"));
		setUseCloseHandler(true);

		addButton(new SpinnerAjaxButton(BUTTON_MARKUP_ID, new ResourceModel("121"), form, Buttons.Type.Outline_Primary)); // register
		addButton(OmModalCloseButton.of());
		add(form);
		add(new Label("register", getString("121")).setRenderBodyOnly(true), new BookmarkablePageLink<>("link", PrivacyPage.class));
		reset(null);
		super.onInitialize();
	}

	public void setSignInDialog(SignInDialog s) {
		this.s = s;
	}

	public void setClientTimeZone() {
		tzModel.setObject(WebSession.get().getClientTZCode());
	}

	public void reset(IPartialPageRequestHandler handler) {
		wasRegistered = false;
		firstName = null;
		lastName = null;
		login = null;
		password = null;
		form.confirmPassword.setModelObject(null);
		email = null;
		lang = WebSession.get().getLanguageByLocale();
		country = WebSession.get().getLocale().getCountry();
		captcha.refresh(handler);
	}

	@Override
	public Modal<String> show(IPartialPageRequestHandler handler) {
		String baseURL = getBaseUrl();
		boolean sendEmailAtRegister = isSendRegisterEmail();
		boolean sendConfirmation = !Strings.isEmpty(baseURL) && isSendVerificationEmail();
		String messageCode = "account.created";
		if (sendConfirmation && sendEmailAtRegister) {
			messageCode = "warn.notverified";
		}
		registerInfo.setModelObject(getString(messageCode));
		handler.add(registerInfo.get("content"));
		reset(handler);
		handler.add(form);
		return super.show(handler);
	}

	@Override
	public void onClose(IPartialPageRequestHandler handler) {
		if (!wasRegistered) {
			s.show(handler);
		}
	}

	@Override
	protected void onDetach() {
		tzModel.detach();
		super.onDetach();
	}

	class RegisterForm extends StatelessForm<Void> {
		private static final long serialVersionUID = 1L;
		private PasswordTextField confirmPassword;
		private PasswordTextField passwordField;
		private RequiredTextField<String> emailField;
		private RequiredTextField<String> loginField;
		private RequiredTextField<String> firstNameField;
		private RequiredTextField<String> lastNameField;

		public RegisterForm(String id) {
			super(id);
			setOutputMarkupId(true);
		}

		@Override
		protected void onInitialize() {
			super.onInitialize();
			add(feedback.setOutputMarkupId(true));
			add(firstNameField = new RequiredTextField<>("firstName", new PropertyModel<>(RegisterDialog.this, "firstName")));
			add(lastNameField = new RequiredTextField<>("lastName", new PropertyModel<>(RegisterDialog.this, "lastName")));
			add(loginField = new RequiredTextField<>("login", new PropertyModel<>(RegisterDialog.this, "login")));
			add(passwordField = new PasswordTextField("password", new PropertyModel<>(RegisterDialog.this, "password")));
			add(confirmPassword = new PasswordTextField("confirmPassword", new Model<>()).setResetPassword(true));
			add(emailField = new RequiredTextField<>("email", new PropertyModel<>(RegisterDialog.this, "email")) {
				private static final long serialVersionUID = 1L;

				@Override
				protected String[] getInputTypes() {
					return new String[] {"email"};
				}
			});
			add(captcha = new Captcha("captcha"));
			firstNameField.setLabel(new ResourceModel("117"));
			lastNameField.setLabel(new ResourceModel("136"));
			loginField.add(minimumLength(getMinLoginLength())).setLabel(new ResourceModel("114"));
			passwordField.setResetPassword(true).add(new StrongPasswordValidator(new User()) {
				private static final long serialVersionUID = 1L;

				@Override
				public void validate(IValidatable<String> pass) {
					User u = new User();
					u.setLogin(loginField.getRawInput());
					u.setAddress(new Address());
					u.getAddress().setEmail(emailField.getRawInput());
					setUser(u);
					super.validate(pass);
				}
			}).setLabel(new ResourceModel("110"));
			confirmPassword.setLabel(new ResourceModel("116"));
			emailField.add(RfcCompliantEmailAddressValidator.getInstance()).setLabel(new ResourceModel("119"));
			AjaxButton ab = new AjaxButton("submit") { // FAKE button so "submit-on-enter" works as expected
				private static final long serialVersionUID = 1L;
			};
			add(ab);
			setDefaultButton(ab);
		}

		@Override
		protected void onValidate() {
			if (passwordField.getConvertedInput() == null
					|| !passwordField.getConvertedInput().equals(confirmPassword.getConvertedInput())) {
				error(getString("232"));
			}
			if (!userDao.checkEmail(emailField.getConvertedInput(), User.Type.USER, null, null)) {
				error(getString("error.email.inuse"));
			}
			if (!userDao.checkLogin(loginField.getConvertedInput(), User.Type.USER, null, null)) {
				error(getString("error.login.inuse"));
			}
			if (hasErrorMessage()) {
				// add random timeout
				try {
					Thread.sleep((long)(10 * Math.random() * 1000));
				} catch (InterruptedException e) {
					log.error("Unexpected exception while sleeting", e);
					Thread.currentThread().interrupt();
				}
			}
		}

		@Override
		protected void onError() {
			RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(this::onError);
		}

		private void onError(AjaxRequestTarget target) {
			target.add(feedback);
		}

		@Override
		protected void onSubmit() {
			RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(this::onSubmit);
		}

		private void onSubmit(AjaxRequestTarget target) {
			wasRegistered = true;
			try {
				Object o = userManager.registerUser(login, password, lastName
						, firstName, email, country, lang, tzModel.getObject());
				if (o instanceof String) {
					registerInfo.setModelObject(getString((String)o));
					target.add(registerInfo.get("content"));
				}
			} catch (Exception e) {
				log.error("[registerUser]", e);
			}
			RegisterDialog.this.close(target);
			registerInfo.show(target);
		}
	}
}
