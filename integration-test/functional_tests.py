import os
import os.path
import errno
import pandas as pd
from jinja2 import Environment, FileSystemLoader
import weasyprint
from test_suite import TestSuite

def get_paysim_jar_path() -> str:
    """
    The method get_paysim_jar_path looks at two places for the paysim.jar because
    you could either use the release version of PaySim or use the IntelliJ.
    The method returns the path to paysim.jar as string.
    """
    root_path = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    artifact_path = os.path.join("classes", "artifacts", "PaySim")
    filename = "paysim.jar"
    file_path = os.path.join(root_path, filename)
    if os.path.isfile(os.path.join(root_path, filename)):
        return os.path.join(root_path, filename)
    elif os.path.isfile(os.path.join(root_path, artifact_path, filename)):
        return os.path.join(root_path, artifact_path, filename)
    else:
        raise FileNotFoundError(errno.ENOENT, os.strerror(errno.ENOENT), filename)

def nice_printing(string: str) -> None:
    """The method nice_printing improves the readability of the console output"""
    print("------------------------")
    print(string)
    print()

if __name__ == "__main__":

    file_path = get_paysim_jar_path()
    dir_path = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

    with open(os.path.join(dir_path, "PaySim.properties")) as file:
        content = file.readlines()

    properties = dict()
    for line in content[1:]:
        pair = line.split("=")
        properties[pair[0]] = pair[1]

    #os.system("java -jar {jar_path}  -file PaySim.properties 1 off".format(jar_path=file_path))

    simulation_name = os.listdir(os.path.join(dir_path, "outputs"))[-1]
    rawLog = pd.read_csv(os.path.join(dir_path, "outputs", simulation_name,
        "{}_rawLog.csv".format(simulation_name)))

    test_suite = TestSuite(rawLog, properties)
    for test in test_suite.tests:
        console_string = test(test_suite)
        nice_printing(console_string)

    template_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "templates")
    css_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "css")
    env = Environment(loader=FileSystemLoader(template_path))
    template = env.get_template("report.html")

    template.stream(test_suite.template_vars).dump("output.html")
    html_output = template.render(test_suite.template_vars)

    weasyprint.HTML(string=html_output).write_pdf("report.pdf",
        stylesheets=[os.path.join(css_path, "typography.css")])
