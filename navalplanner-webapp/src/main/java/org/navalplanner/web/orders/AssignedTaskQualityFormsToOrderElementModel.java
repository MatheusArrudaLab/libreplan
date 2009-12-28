/*
 * This file is part of ###PROJECT_NAME###
 *
 * Copyright (C) 2009 Fundación para o Fomento da Calidade Industrial e
 *                    Desenvolvemento Tecnolóxico de Galicia
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

package org.navalplanner.web.orders;

import static org.navalplanner.web.I18nHelper._;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.validator.InvalidValue;
import org.navalplanner.business.common.exceptions.ValidationException;
import org.navalplanner.business.orders.daos.IOrderElementDAO;
import org.navalplanner.business.orders.entities.OrderElement;
import org.navalplanner.business.qualityforms.daos.IQualityFormDAO;
import org.navalplanner.business.qualityforms.entities.QualityForm;
import org.navalplanner.business.qualityforms.entities.TaskQualityForm;
import org.navalplanner.business.qualityforms.entities.TaskQualityFormItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AssignedTaskQualityFormsToOrderElementModel implements
        IAssignedTaskQualityFormsToOrderElementModel {

    @Autowired
    private IOrderElementDAO orderDAO;

    @Autowired
    private IQualityFormDAO qualityFormDAO;

    private OrderElement orderElement;

    private IOrderModel orderModel;

    @Override
    public OrderElement getOrderElement() {
        return orderElement;
    }

    @Override
    public void setOrderElement(OrderElement orderElement) {
        this.orderElement = orderElement;
    }

    @Override
    @Transactional(readOnly = true)
    public void init(OrderElement orderElement) {
        this.orderElement = orderElement;
        initializeOrderElement(this.orderElement);
    }

    private void initializeOrderElement(OrderElement orderElement) {
        reattachQualityForms();
        orderDAO.reattach(orderElement);
        orderElement.getName();
        initializeTaskQualityForms(orderElement.getTaskQualityForms());
    }

    private void reattachQualityForms() {
        for (QualityForm qualityForm : orderModel.getQualityForms()) {
            qualityFormDAO.reattach(qualityForm);
        }
    }

    private void initializeTaskQualityForms(
            Collection<TaskQualityForm> taskQualityForms) {
        for (TaskQualityForm taskQualityForm : taskQualityForms) {
            taskQualityForm.getQualityForm().getName();
            initializeTaskQualityFormItems(taskQualityForm
                    .getTaskQualityFormItems());
        }
    }

    public void initializeTaskQualityFormItems(
            Collection<TaskQualityFormItem> taskQualityFormItems) {
        for (TaskQualityFormItem taskQualityFormItem : taskQualityFormItems) {
            taskQualityFormItem.getName();
        }
    }

    @Override
    public List<QualityForm> getNotAssignedQualityForms() {
        List<QualityForm> result = new ArrayList<QualityForm>();
        if (orderElement != null) {
            return getlistNotAssignedQualityForms();
        }
        return result;
    }

    private List<QualityForm> getlistNotAssignedQualityForms() {
        List<QualityForm> result = new ArrayList<QualityForm>();
        for (QualityForm qualityForm : orderModel.getQualityForms()) {
            if (!isAssigned(qualityForm)) {
                result.add(qualityForm);
            }
        }
        return result;
    }

    @Override
    public List<TaskQualityForm> getTaskQualityForms() {
        List<TaskQualityForm> result = new ArrayList<TaskQualityForm>();
        if (orderElement != null) {
            result.addAll(orderElement.getTaskQualityForms());
        }
        return result;
    }

    @Override
    public void assignTaskQualityForm(QualityForm qualityForm) {
        orderElement.addTaskQualityForm(qualityForm);
    }

    @Override
    public void deleteTaskQualityForm(TaskQualityForm taskQualityForm) {
        orderElement.removeTaskQualityForm(taskQualityForm);
    }

    @Override
    public boolean isAssigned(QualityForm qualityForm) {
        for (TaskQualityForm taskQualityForm : orderElement
                .getTaskQualityForms()) {
            if (qualityForm.equals(taskQualityForm.getQualityForm())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setOrderModel(IOrderModel orderModel) {
        this.orderModel = orderModel;
    }

    public boolean isDisabledPassedItem(TaskQualityForm taskQualityForm,
            TaskQualityFormItem item) {
        if ((taskQualityForm == null) || ((item == null))) {
            return true;
        }
        return (!(item.getPassed() || taskQualityForm
                .isPassedPreviousItem(item)));
    }

    public boolean isDisabledDateItem(TaskQualityForm taskQualityForm,
            TaskQualityFormItem item) {
        if ((taskQualityForm == null) || ((item == null))) {
            return true;
        }
        return (!item.getPassed());
    }

    public void updatePassedTaskQualityFormItems(TaskQualityForm taskQualityForm) {
        if (taskQualityForm != null) {
            Integer position = getFirstNotPassedPosition(taskQualityForm);
            List<TaskQualityFormItem> items = taskQualityForm
                .getTaskQualityFormItems();
            for (int i = position; i < items.size(); i++) {
                items.get(i).setPassed(false);
                items.get(i).setDate(null);
            }
        }
    }

    private Integer getFirstNotPassedPosition(TaskQualityForm taskQualityForm) {
        Integer position = 0;
        for (TaskQualityFormItem item : taskQualityForm
                .getTaskQualityFormItems()) {
            if (!item.getPassed()) {
                return position;
            }
            position++;
        }
        return position;
    }

    // Operation to confirm and validate

    @Override
    public void validate() {
        if (getOrderElement() != null) {
            for (TaskQualityForm taskQualityForm : orderElement
                    .getTaskQualityForms()) {
                validateTaskQualityForm(taskQualityForm);
            }
        }
    }

    private void validateTaskQualityForm(TaskQualityForm taskQualityForm) {
        validateTaskQualityFormItems(taskQualityForm);
    }

    private void validateTaskQualityFormItems(TaskQualityForm taskQualityForm) {
        for (TaskQualityFormItem item : taskQualityForm
                .getTaskQualityFormItems()) {

            if (!taskQualityForm.isCorrectConsecutivePassed(item)) {
                throw new ValidationException(new InvalidValue(
                        _("must be consecutive"), TaskQualityForm.class,
                        "passed", item.getName(), taskQualityForm));
            }

            if (!taskQualityForm.isCorrectConsecutiveDate(item)) {
                throw new ValidationException(new InvalidValue(
                        _("must be consecutive"), TaskQualityForm.class,
                        "date", item.getName(), taskQualityForm));
            }

            if (!item.checkConstraintIfDateCanBeNull()) {
                throw new ValidationException(new InvalidValue(
                        _("cannot be null"), TaskQualityForm.class, "date",
                        item.getName(), taskQualityForm));
            }
        }
    }

}
