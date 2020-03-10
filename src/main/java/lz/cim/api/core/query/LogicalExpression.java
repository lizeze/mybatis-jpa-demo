package lz.cim.api.core.query;



import lz.cim.api.core.query.searchmodel.QueryMethod;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;


public class LogicalExpression implements Criterion {
    private Criterion[] criterion;  // 逻辑表达式中包含的表达式
    private QueryMethod operator;      //计算符

    public LogicalExpression(Criterion[] criterions, QueryMethod operator) {
        this.criterion = criterions;
        this.operator = operator;
    }
    @Override
    public Predicate toPredicate(Root<?> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        List<Predicate> predicates = new ArrayList<Predicate>();
        for(int i=0;i<this.criterion.length;i++){
            predicates.add(this.criterion[i].toPredicate(root, query, builder));
        }
        switch (operator) {
            case OR:
                return builder.or(predicates.toArray(new Predicate[predicates.size()]));
            default:
                return null;
        }
    }
}
