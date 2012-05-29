/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2012 Igalia, S.L.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.libreplan.web.users.dashboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.NonUniqueResultException;
import org.joda.time.LocalDate;
import org.libreplan.business.calendars.entities.ResourceCalendar;
import org.libreplan.business.common.daos.IConfigurationDAO;
import org.libreplan.business.common.exceptions.InstanceNotFoundException;
import org.libreplan.business.costcategories.entities.TypeOfWorkHours;
import org.libreplan.business.orders.daos.ISumChargedEffortDAO;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.planner.daos.IResourceAllocationDAO;
import org.libreplan.business.planner.entities.SpecificResourceAllocation;
import org.libreplan.business.resources.entities.Worker;
import org.libreplan.business.scenarios.IScenarioManager;
import org.libreplan.business.users.entities.User;
import org.libreplan.business.workingday.EffortDuration;
import org.libreplan.business.workingday.IntraDayDate.PartialDay;
import org.libreplan.business.workreports.daos.IWorkReportDAO;
import org.libreplan.business.workreports.daos.IWorkReportTypeDAO;
import org.libreplan.business.workreports.entities.PredefinedWorkReportTypes;
import org.libreplan.business.workreports.entities.WorkReport;
import org.libreplan.business.workreports.entities.WorkReportLine;
import org.libreplan.business.workreports.entities.WorkReportType;
import org.libreplan.web.UserUtil;
import org.libreplan.web.calendars.BaseCalendarModel;
import org.libreplan.web.common.concurrentdetection.OnConcurrentModification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Model for creation/edition of a monthly timesheet
 *
 * @author Manuel Rego Casasnovas <mrego@igalia.com>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@OnConcurrentModification(goToPage = "/myaccount/userDashboard.zul")
public class MonthlyTimesheetModel implements IMonthlyTimesheetModel {

    private User user;

    private LocalDate date;

    private List<OrderElement> orderElements;

    private WorkReport workReport;

    private Map<LocalDate, EffortDuration> capacityMap;

    @Autowired
    private IResourceAllocationDAO resourceAllocationDAO;

    @Autowired
    private IScenarioManager scenarioManager;

    @Autowired
    private IWorkReportDAO workReportDAO;

    @Autowired
    private IWorkReportTypeDAO workReportTypeDAO;

    @Autowired
    private ISumChargedEffortDAO sumChargedEffortDAO;

    @Autowired
    private IConfigurationDAO configurationDAO;

    @Override
    @Transactional(readOnly = true)
    public void initCreateOrEdit(LocalDate date) {
        user = UserUtil.getUserFromSession();
        if (!user.isBound()) {
            throw new RuntimeException(
                    "This page only can be used by users bound to a resource");
        }
        this.date = date;

        initCapacityMap();

        initOrderElements();
        initWorkReport();
    }

    private void initCapacityMap() {
        forceLoad(getWorker().getCalendar());

        LocalDate date = getDate();
        LocalDate start = date.dayOfMonth().withMinimumValue();
        LocalDate end = date.dayOfMonth().withMaximumValue();

        capacityMap = new HashMap<LocalDate, EffortDuration>();
        for (LocalDate day = start; day.compareTo(end) <= 0; day = day
                .plusDays(1)) {
            capacityMap.put(
                    day,
                    getWorker().getCalendar().getCapacityOn(
                            PartialDay.wholeDay(day)));
        }
    }

    private void forceLoad(ResourceCalendar calendar) {
        BaseCalendarModel.forceLoadBaseCalendar(calendar);
    }

    private void initWorkReport() {
        // Get work report representing this monthly timesheet
        workReport = workReportDAO.getMonthlyTimesheetWorkReport(
                user.getWorker(), date);
        if (workReport == null) {
            // If it doesn't exist yet create a new one
            workReport = WorkReport
                    .create(getMonthlyTimesheetsWorkReportType());
            workReport.setCodeAutogenerated(true);
            workReport.setResource(user.getWorker());
        }
        forceLoad(workReport.getWorkReportType());
    }

    private WorkReportType getMonthlyTimesheetsWorkReportType() {
        try {
            WorkReportType workReportType = workReportTypeDAO
                    .findUniqueByName(PredefinedWorkReportTypes.MONTHLY_TIMESHEETS
                            .getName());
            return workReportType;
        } catch (NonUniqueResultException e) {
            throw new RuntimeException(e);
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void forceLoad(WorkReportType workReportType) {
        workReportType.getLineFields().size();
        workReportType.getWorkReportLabelTypeAssigments().size();
        workReportType.getHeadingFields().size();
    }

    private void initOrderElements() {
        List<SpecificResourceAllocation> resourceAllocations = resourceAllocationDAO
                .findSpecificAllocationsRelatedTo(scenarioManager.getCurrent(),
                        UserDashboardUtil.getBoundResourceAsList(user), null,
                        null);

        orderElements = new ArrayList<OrderElement>();
        for (SpecificResourceAllocation each : resourceAllocations) {
            OrderElement orderElement = each.getTask().getOrderElement();
            forceLoad(orderElement);
            orderElements.add(orderElement);
        }
    }

    private void forceLoad(OrderElement orderElement) {
        orderElement.getOrder().getName();
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

    @Override
    public Worker getWorker() {
        return user.getWorker();
    }

    @Override
    public List<OrderElement> getOrderElements() {
        return orderElements;
    }

    @Override
    public EffortDuration getEffortDuration(OrderElement orderElement,
            LocalDate date) {
        WorkReportLine workReportLine = getWorkReportLine(orderElement, date);
        if (workReportLine == null) {
            return null;
        }
        return workReportLine.getEffort();
    }

    private WorkReportLine getWorkReportLine(OrderElement orderElement,
            LocalDate date) {
        for (WorkReportLine line : workReport.getWorkReportLines()) {
            if (line.getOrderElement().equals(orderElement)
                    && LocalDate.fromDateFields(line.getDate()).equals(date)) {
                return line;
            }
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public void setEffortDuration(OrderElement orderElement, LocalDate date,
            EffortDuration effortDuration) {
        WorkReportLine workReportLine = getWorkReportLine(orderElement, date);
        if (workReportLine == null) {
            workReportLine = WorkReportLine.create(workReport);
            workReportLine.setOrderElement(orderElement);
            workReportLine.setDate(date.toDateTimeAtStartOfDay().toDate());
            workReportLine.setTypeOfWorkHours(getTypeOfWorkHours());
            workReport.addWorkReportLine(workReportLine);
        }
        workReportLine.setEffort(effortDuration);
    }

    private TypeOfWorkHours getTypeOfWorkHours() {
        return configurationDAO.getConfiguration()
                .getMonthlyTimesheetsTypeOfWorkHours();
    }

    @Override
    @Transactional
    public void save() {
        if (workReport.getWorkReportLines().isEmpty()
                && workReport.isNewObject()) {
            // Do nothing.
            // A new work report if it doesn't have work report lines is not
            // saved as it will not be possible to find it later with
            // WorkReportDAO.getMonthlyTimesheetWorkReport() method.
        } else {
            sumChargedEffortDAO
                    .updateRelatedSumChargedEffortWithWorkReportLineSet(workReport
                            .getWorkReportLines());
            workReportDAO.save(workReport);
        }
    }

    @Override
    public void cancel() {
        user = null;
        date = null;
        orderElements = null;
        workReport = null;
    }

    @Override
    public EffortDuration getEffortDuration(OrderElement orderElement) {
        EffortDuration result = EffortDuration.zero();
        for (WorkReportLine line : workReport.getWorkReportLines()) {
            if (line.getOrderElement().equals(orderElement)) {
                result = result.plus(line.getEffort());
            }
        }
        return result;
    }

    @Override
    public EffortDuration getEffortDuration(LocalDate date) {
        EffortDuration result = EffortDuration.zero();
        for (WorkReportLine line : workReport.getWorkReportLines()) {
            if (LocalDate.fromDateFields(line.getDate()).equals(date)) {
                result = result.plus(line.getEffort());
            }
        }
        return result;
    }

    @Override
    public EffortDuration getTotalEffortDuration() {
        return workReport.getTotalEffortDuration();
    }

    @Override
    public EffortDuration getResourceCapacity(LocalDate date) {
        return capacityMap.get(date);
    }

}
