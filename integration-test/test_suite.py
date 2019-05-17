import csv

class FunctionSet(set):
    """
    The class FunctionSet allows me to store functions in a set. With the
    decorator register a method is added to the set.
    """
    def register(self, method):
        self.add(method)
        return method

class TestSuite:
    """
    The class TestSuite holds all the methods that we want to test. Since we want to
    test equality, inequality of singular values and have summaries of attributes.
    """
    tests = FunctionSet()

    def __init__(self, rawLog, properties):
        self.rawLog = rawLog
        self.properties = properties
        self.template_vars = {"simple_value_tests": [],
                              "simple_inequality_tests": [],
                              "attribute_summaries": []}

    def simple_value_test(self, simulation_result, expected, attribute):

        test_result = simulation_result == expected
        jinja_template_vars = {"in_simulation": simulation_result,
                               "expected": expected,
                               "test_result": test_result,
                               "attribute": attribute}
        self.template_vars["simple_value_tests"].append(jinja_template_vars)

        return "Simulation {} {} equals given {} {}? {}".format(
            attribute, simulation_result, attribute, expected, test_result)

    def simple_inequality_test(self, simulation_result, expected, isSmaller, attribute):
        test_result = simulation_result < expected if isSmaller else simulation_result > expected
        jinja_template_vars = {"in_simulation": simulation_result,
                                "inequality": "<" if isSmaller else ">",
                               "expected": expected,
                               "test_result": test_result,
                               "attribute": attribute}
        self.template_vars["simple_inequality_tests"].append(jinja_template_vars)

        return "Simulation {} {} {} given {} {}? {}".format(
            attribute, simulation_result, "smaller" if isSmaller else "larger",
            attribute, expected, test_result)

    def summary_attribute(self, data, attribute):
        data_infos = data.describe()
        jinja_template_vars = {"infos": data_infos,
                               "attribute": attribute}
        self.template_vars["attribute_summaries"].append(jinja_template_vars)

        return "Simulation {}: \n \
                \t mean: {:.2f} \n \
                \t min: {:.2f} \n \
                \t max: {:.2f}".format(attribute, data_infos["mean"], data_infos["min"], data_infos["max"])


    @tests.register
    def test_number_of_steps(self):

        steps_in_simulation = self.rawLog.tail(1).values[0,0] + 1
        steps_expected = int(self.properties["nbSteps"])
        return self.simple_value_test(steps_in_simulation,
            steps_expected, "steps")

    @tests.register
    def test_number_of_clients(self):
        number_clients_in_simulation = len(self.rawLog[self.rawLog["nameOrig"].map(
            lambda string: not string.startswith("CC"))]["nameOrig"].unique())
        number_clients_expected = int(self.properties["nbClients"])
        return self.simple_value_test(number_clients_in_simulation,
            number_clients_expected, "number of clients")

    @tests.register
    def test_overdraft_limits(self):
        mask_mules = self.rawLog["nameOrig"].map(lambda string: not string.startswith("CC"))
        minimum_clients_end_balance = self.rawLog[mask_mules].groupby("nameOrig").tail(1)["newBalanceOrig"].min()

        file_path = self.properties["overdraftLimits"]
        with open(file_path.strip()) as csvfile:
            overdraft_data = list(csv.reader(csvfile))
            maximum_overdraft = int(overdraft_data[-1][-1])

        return self.simple_inequality_test(minimum_clients_end_balance, maximum_overdraft, False, "overdraft")

    @tests.register
    def test_summary_balances(self):
        mask_mules = self.rawLog["nameOrig"].map(lambda string: not string.startswith("CC"))
        clients_end_balance = self.rawLog[mask_mules].groupby("nameOrig").tail(1)["newBalanceOrig"]

        return self.summary_attribute(clients_end_balance, "client balance")

    @tests.register
    def test_summary_amounts(self):
        mask_mules = self.rawLog["nameOrig"].map(lambda string: not string.startswith("CC"))
        transactions_amount = self.rawLog[mask_mules]["amount"]

        return self.summary_attribute(transactions_amount, "transaction amount")

    @tests.register
    def test_money_sink(self):
        mask_mules = self.rawLog["nameOrig"].map(lambda string: not string.startswith("CC"))
        start_budget = self.rawLog[mask_mules].groupby("nameOrig").head(1)["oldBalanceOrig"].sum()
        end_budget =  self.rawLog[mask_mules].groupby("nameOrig").tail(1)["newBalanceOrig"].sum()

        return self.simple_inequality_test(start_budget, end_budget, True, "money pullout")

    @tests.register
    def test_more_than_half_over_zero(self):
        mask_mules = self.rawLog["nameOrig"].map(lambda string: not string.startswith("CC"))
        clients_end_balance =  self.rawLog[mask_mules].groupby("nameOrig").tail(1)["newBalanceOrig"]
        clients_with_positive_balance = (clients_end_balance > 0).sum()
        number_clients= int(self.properties["nbClients"])

        return self.simple_inequality_test(clients_with_positive_balance, number_clients/2, False, "end balance > 0")

    @tests.register
    def test_fraud_probability(self):
        simulation_fraud_percentage = self.rawLog["isFraud"].sum() / self.rawLog["isFraud"].count()
        parameter_fraud_percentage = float(self.properties["fraudProbability"])

        return self.simple_value_test(simulation_fraud_percentage, parameter_fraud_percentage, "fraud probability")
