/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
 * Copyright (C) 2010-2012 Igalia, S.L.
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

package org.libreplan.web.orders;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.libreplan.business.calendars.entities.BaseCalendar;
import org.libreplan.business.externalcompanies.entities.EndDateCommunication;
import org.libreplan.business.externalcompanies.entities.ExternalCompany;
import org.libreplan.business.labels.entities.Label;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.business.orders.entities.OrderElement;
import org.libreplan.business.orders.entities.OrderLineGroup;
import org.libreplan.business.planner.entities.PositionConstraintType;
import org.libreplan.business.qualityforms.entities.QualityForm;
import org.libreplan.business.resources.entities.Criterion;
import org.libreplan.business.resources.entities.CriterionType;
import org.libreplan.business.templates.entities.OrderElementTemplate;
import org.libreplan.business.templates.entities.OrderTemplate;
import org.libreplan.web.common.IIntegrationEntityModel;
import org.libreplan.web.planner.order.PlanningStateCreator.PlanningState;
import org.zkoss.ganttz.IPredicate;
import org.zkoss.zk.ui.Desktop;

/**
 * Contract for {@link OrderModel}<br />
 *
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 * @author Diego Pino García <dpino@igalia.com>
 * @author Manuel Rego Casasnovas <rego@igalia.com>
 */
public interface IOrderModel extends IIntegrationEntityModel {

    /**
     * Adds {@link Label} to list of labels
     *
     * @param label
     */
    void addLabel(Label label);

    /**
     * Returns a list of {@link Label}
     *
     * @return
     */
    List<Criterion> getCriterionsFor(CriterionType criterionType);

    Map<CriterionType, List<Criterion>> getMapCriterions();

    List<Label> getLabels();

    List<QualityForm> getQualityForms();

    Order getOrder();

    IOrderElementModel getOrderElementModel(OrderElement orderElement);

    /**
     * Iterates through all the orderElements of an order, and checks if
     * orderElement holds predicate. In case it is true, adds orderElement.
     */
    OrderElementTreeModel getOrderElementsFilteredByPredicate(IPredicate predicate);

    OrderElementTreeModel getOrderElementTreeModel();

    List<Order> getOrders();

    void initEdit(Order order, Desktop desktop);

    void prepareForCreate(Desktop desktop);

    void remove(Order order);

    void save();

    void save(boolean showSaveMessage);

    void setPlanningState(PlanningState planningState);

    List<BaseCalendar> getBaseCalendars();

    BaseCalendar getDefaultCalendar();

    BaseCalendar getCalendar();

    void setCalendar(BaseCalendar calendar);

    boolean isCodeAutogenerated();

    void prepareCreationFrom(OrderTemplate template, Desktop desktop);

    OrderElement createFrom(OrderLineGroup parent, OrderElementTemplate template);

    List<ExternalCompany> getExternalCompaniesAreClient();

    void setExternalCompany(ExternalCompany externalCompany);

    public String gettooltipText(Order order);

    List<Order> getFilterOrders(OrderPredicate predicate);

    boolean userCanRead(Order order, String loginName);

    boolean userCanWrite(Order order);

    boolean isAlreadyInUse(OrderElement orderElement);

    boolean isAlreadyInUseAndIsOnlyInCurrentScenario(Order order);

    void useSchedulingDataForCurrentScenario(Order order);

    PlanningState getPlanningState();

    boolean hasImputedExpenseSheetsThisOrAnyOfItsChildren(OrderElement order);

    void removeAskedEndDate(EndDateCommunication endDate);

    SortedSet<EndDateCommunication> getEndDates();

    void addAskedEndDate(Date value);

    boolean alreadyExistsRepeatedEndDate(Date value);

    boolean isAnyTaskWithConstraint(PositionConstraintType type);

    boolean isOnlyChildAndParentAlreadyInUseByHoursOrExpenses(
            OrderElement orderElement);

    boolean isJiraActivated();

}
