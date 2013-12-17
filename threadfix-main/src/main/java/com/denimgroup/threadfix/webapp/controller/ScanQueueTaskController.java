////////////////////////////////////////////////////////////////////////
//
//     Copyright (c) 2009-2013 Denim Group, Ltd.
//
//     The contents of this file are subject to the Mozilla Public License
//     Version 2.0 (the "License"); you may not use this file except in
//     compliance with the License. You may obtain a copy of the License at
//     http://www.mozilla.org/MPL/
//
//     Software distributed under the License is distributed on an "AS IS"
//     basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//     License for the specific language governing rights and limitations
//     under the License.
//
//     The Original Code is ThreadFix.
//
//     The Initial Developer of the Original Code is Denim Group, Ltd.
//     Portions created by Denim Group, Ltd. are Copyright (C)
//     Denim Group, Ltd. All Rights Reserved.
//
//     Contributor(s): Denim Group, Ltd.
//
////////////////////////////////////////////////////////////////////////

package com.denimgroup.threadfix.webapp.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.denimgroup.threadfix.data.entities.Permission;
import com.denimgroup.threadfix.data.entities.ScanQueueTask;
import com.denimgroup.threadfix.service.ApplicationService;
import com.denimgroup.threadfix.service.PermissionService;
import com.denimgroup.threadfix.service.SanitizedLogger;
import com.denimgroup.threadfix.service.ScanQueueService;

@Controller
@RequestMapping("/configuration/scanqueue")
public class ScanQueueTaskController {

	private final SanitizedLogger log = new SanitizedLogger(ScanQueueTaskController.class);
	
	private ScanQueueService scanQueueService;
	private PermissionService permissionService;
	
	@Autowired
	public ScanQueueTaskController(ScanQueueService scanQueueService,
			PermissionService permissionService,
			ApplicationService applicationService) {
		this.scanQueueService = scanQueueService;
		this.permissionService = permissionService;
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public String index(HttpServletRequest request, Model model) {
		model.addAttribute("scanQueueTaskList", scanQueueService.loadAll());
		
		
		return "config/scanqueue/index";
	}
	
	@RequestMapping(value = "/{scanQueueTaskId}/detail", method = RequestMethod.GET)
	public String showDetail(@PathVariable("scanQueueTaskId") int scanQueueTaskId, Model model,
			HttpServletRequest request) {
		
		model.addAttribute("scanQueueTask", scanQueueService.retrieveById(scanQueueTaskId));
		
		return "config/scanqueue/detail";
	}
	
	@RequestMapping(value = "/organizations/{orgId}/applications/{appId}/addScanQueueTask", method = RequestMethod.POST)
	public String addScanQueueTask(@PathVariable("appId") int appId, @PathVariable("orgId") int orgId,
			@RequestParam("scanQueueType") String scanQueueType,
			HttpServletRequest request, Model model) {
		
		log.info("Start adding scan task to application " + appId);
		if (!permissionService.isAuthorized(Permission.CAN_MANAGE_APPLICATIONS,orgId,appId)){
			return "403";
		}
		ScanQueueTask task = scanQueueService.queueScan(appId, scanQueueType);
		
		if (task == null) {
			ControllerUtils.addErrorMessage(request,
					"There was something wrong when we tried adding task...");
			model.addAttribute("contentPage", "/organizations/" + orgId + "/applications/" + appId);
			return "ajaxFailureHarness";
		}
		
		ControllerUtils.addSuccessMessage(request,
				"Task ID " + task.getId() + " was successfully added to the application.");
		model.addAttribute("contentPage", "/organizations/" + orgId + "/applications/" + appId);
		log.info("Ended adding scan task to application " + appId);
		return "ajaxRedirectHarness";
	}

	@RequestMapping(value = "/organizations/{orgId}/applications/{appId}/scanQueueTask/{taskId}/delete", method = RequestMethod.POST)
	public String deleteScanQueueTask(@PathVariable("appId") int appId,
			@PathVariable("orgId") int orgId,
			@PathVariable("taskId") int taskId,
			HttpServletRequest request, Model model) {
		
		log.info("Start deleting scan task from application " + appId);
		if (!permissionService.isAuthorized(Permission.CAN_MANAGE_APPLICATIONS,orgId,appId)){
			return "403";
		}
		ScanQueueTask task = scanQueueService.loadTaskById(taskId);
		if (task == null) {
			ControllerUtils.addSuccessMessage(request,
					"The task was successfully added to the application.");
			model.addAttribute("commentError", "The task submitted was invalid");
			model.addAttribute("contentPage", "applications/tabs/scanQueueTab.jsp");
			return "ajaxFailureHarness";
		}
		String ret = scanQueueService.deleteTask(task);
		if (ret != null) {
			ControllerUtils.addErrorMessage(request, ret);
		} else {
			ControllerUtils.addSuccessMessage(request,
					"Task ID " + taskId + " was successfully deleted");
		}
		log.info("Ended deleting scan task from application " + appId);
		
		return "redirect:/organizations/" + orgId + "/applications/" + appId;
	}
	
}
