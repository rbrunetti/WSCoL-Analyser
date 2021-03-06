package it.polimi.specl.declaration;

import it.polimi.specl.assertions.AssertionService;
import it.polimi.specl.dataobject.DataObject;
import it.polimi.specl.helpers.FunctionHelper;
import it.polimi.specl.helpers.StringHelper;
import it.polimi.specl.helpers.VariablesHelper;
import it.polimi.specl.specl.AssertionQuantified;
import it.polimi.specl.specl.Constant;
import it.polimi.specl.specl.Declaration;
import it.polimi.specl.specl.Expression;
import it.polimi.specl.specl.Value;
import it.polimi.specl.specl.Values;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;

public class DeclarationServiceImpl implements DeclarationService{

	private AssertionService as;
	
	private static Logger logger = Logger.getRootLogger();
	
	public DeclarationServiceImpl(AssertionService as){
		this.as = as;
//		logger.getLoggerRepository().resetConfiguration();
//		logger.addAppender(new ConsoleAppender(new PatternLayout("%5p - %m%n")));
	}

	/**
	 * Set variables according to the {@link Declaration}. Evaluates the query, resolves the variables, translate {@link Values} into {@link DataObject}, extract correct value from a {@link Constant} and assigns the values to a key with the specified name The values are extracted and saved as simple type ({@link String}, {@link Double} and {@link Boolean}) if the results of an evaluation is a {@link DataObject} with a single value. Otherwise as a {@link DataObject}.
	 * 
	 * @param declarations
	 *            the list of {@link Declaration} rules parsed
	 * @throws Exception
	 *             if the variable is already in use
	 * @throws Exception
	 *             if a variable, used inside a declaration and on which that is based, is not defined
	 * @throws Exception
	 *             if the evaluation goes wrong, the cause will be specified
	 * @throws Exception
	 *             if the evaluation gives back an empty result
	 */
	@Override
	public void setVariable(EObjectContainmentEList<Declaration> declarations) throws Exception {
		Object result = null;
		
		if(logger.isInfoEnabled()){
			logger.info("DECLARATION EVALUATION");
		}
		
		for (Declaration d : declarations) {
			String assertionRep = StringHelper.declarationToString(d);
			if (VariablesHelper.getVariable(d.getVar()) != null) {
				throw new Exception("The variable '" + d.getVar() + "' in '" + StringHelper.declarationToString(d) + "' is already used (" + d.getVar() + " = " + VariablesHelper.getVariable(d.getVar()) + "). Choose another [token: '" + assertionRep + "']");
			}
			
			if (d.getAssert() instanceof Constant) {
				if (((Constant) d.getAssert()).getString() != null) {
					result = ((Constant) d.getAssert()).getString();
				} else {
					result = ((Constant) d.getAssert()).getNumber();
				}
			} else if (d.getAssert().getValues() != null) {
				List<Object> values = new ArrayList<Object>();
				for (Value v : d.getAssert().getValues().getValue()) {
					if (!v.getSteps().isEmpty()) {
						try {
							values.add(FunctionHelper.applyFunctions(as.resolveQuery(v.getSteps()), v.getFunctions()));
						} catch (Exception e) {
							throw new Exception(e.getMessage() + " [token: '" + assertionRep + "']");
						}
					} else if (v instanceof Constant) {
						if (((Constant) v).getString() != null) {
							values.add(((Constant) v).getString());
						} else {
							values.add(((Constant) v).getNumber());
						}
					} else {
						values.add(v);
					}
				}
				result = values;
			} else if (!d.getAssert().getSteps().isEmpty()) {
				try {
					result = as.resolveQuery(d.getAssert().getSteps());
					// *** FUNCTIONS ***
					result = FunctionHelper.applyFunctions(result, d.getAssert().getFunctions());
				} catch (Exception e) {
					throw new Exception(e.getMessage() + " [token: '" + assertionRep + "']");
				}
			} else if (d.getAssert() instanceof AssertionQuantified) {
				result = as.doAssertionQuantified(d.getAssert());
			} else if (d.getAssert() instanceof Expression) {
				result = as.resolveExpression(d.getAssert());
			} else {
				result = d.getAssert().isBoolean();
			}

			VariablesHelper.putVariable(d.getVar(), result);
			
			if(logger.isInfoEnabled()){
				logger.info(StringHelper.declarationToString(d));
			}
		}
		return;
	}

}
