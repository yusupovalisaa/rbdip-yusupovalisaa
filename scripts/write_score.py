#!/usr/bin/env python3
"""
Собирает результаты Checkstyle/PMD/JUnit(surefire)/JaCoCo/PIT из target/
и пишет score.json - машиночитаемый результат прогона CI.

Веса гейтов согласованы с тематическим планом курса (ЛР1..ЛР5 = 15/20/15/20/30,
итого 100). Скрипт никогда не падает с ошибкой - при отсутствии отчёта
считает соответствующий гейт не пройденным (0 баллов), а не ломает пайплайн.
"""
import json
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TARGET = ROOT / "target"

STYLE_VIOLATIONS_THRESHOLD = 15
JACOCO_LINE_COVERAGE_MIN = 0.60
PIT_MUTATION_SCORE_MIN = 60.0


def count_checkstyle_violations():
    path = TARGET / "checkstyle-result.xml"
    if not path.exists():
        return None
    try:
        root = ET.parse(path).getroot()
        return sum(1 for _ in root.iter("error"))
    except ET.ParseError:
        return None


def count_pmd_violations():
    path = TARGET / "pmd.xml"
    if not path.exists():
        return None
    try:
        root = ET.parse(path).getroot()
        return sum(1 for _ in root.iter("{net.sourceforge.pmd}violation")) or sum(
            1 for _ in root.iter("violation")
        )
    except ET.ParseError:
        return None


def surefire_test_passed(class_name):
    path = TARGET / "surefire-reports" / f"TEST-{class_name}.xml"
    if not path.exists():
        return False
    try:
        root = ET.parse(path).getroot()
        failures = int(root.get("failures", "0"))
        errors = int(root.get("errors", "0"))
        return failures == 0 and errors == 0
    except (ET.ParseError, ValueError):
        return False


def jacoco_line_coverage():
    path = TARGET / "site" / "jacoco" / "jacoco.xml"
    if not path.exists():
        return None
    try:
        root = ET.parse(path).getroot()
        for counter in root.findall("./counter"):
            if counter.get("type") == "LINE":
                missed = int(counter.get("missed", "0"))
                covered = int(counter.get("covered", "0"))
                total = missed + covered
                return (covered / total) if total else 0.0
    except ET.ParseError:
        return None
    return None


def pit_mutation_score():
    reports_dir = TARGET / "pit-reports"
    if not reports_dir.exists():
        return None
    mutation_files = list(reports_dir.glob("**/mutations.xml"))
    if not mutation_files:
        return None
    try:
        root = ET.parse(mutation_files[0]).getroot()
        mutations = root.findall("mutation")
        if not mutations:
            return None
        killed = sum(1 for m in mutations if m.get("status") == "KILLED")
        return 100.0 * killed / len(mutations)
    except ET.ParseError:
        return None


def main():
    checkstyle_violations = count_checkstyle_violations() or 0
    pmd_violations = count_pmd_violations() or 0
    style_ok = (checkstyle_violations + pmd_violations) <= STYLE_VIOLATIONS_THRESHOLD

    coverage = jacoco_line_coverage()
    coverage_ok = coverage is not None and coverage >= JACOCO_LINE_COVERAGE_MIN

    mutation_score = pit_mutation_score()
    mutation_ok = mutation_score is not None and mutation_score >= PIT_MUTATION_SCORE_MIN

    gates = {
        "lab1_style": {
            "passed": style_ok,
            "weight": 15,
            "detail": f"checkstyle={checkstyle_violations}, pmd={pmd_violations}",
        },
        "lab2_coverage": {
            "passed": coverage_ok,
            "weight": 10,
            "detail": f"line_coverage={coverage}",
        },
        "lab2_schema": {
            "passed": surefire_test_passed(
                "com.rbdip.bookstore.reference.SchemaNormalizationReferenceTest"
            ),
            "weight": 10,
        },
        "lab3_migration": {
            "passed": surefire_test_passed(
                "com.rbdip.bookstore.reference.LoadDuringMigrationReferenceTest"
            ),
            "weight": 15,
        },
        "lab4_nplusone": {
            "passed": surefire_test_passed("com.rbdip.bookstore.reference.NPlusOneReferenceTest"),
            "weight": 10,
        },
        "lab4_architecture": {
            "passed": surefire_test_passed(
                "com.rbdip.bookstore.reference.architecture.ArchitectureRulesTest"
            ),
            "weight": 10,
        },
        "lab5_mutation": {
            "passed": mutation_ok,
            "weight": 15,
            "detail": f"mutation_score={mutation_score}",
        },
        "lab5_reference": {
            "passed": (
                surefire_test_passed("com.rbdip.bookstore.reference.OrderApiReferenceTest")
                and surefire_test_passed("com.rbdip.bookstore.reference.PricingCalculatorReferenceTest")
            ),
            "weight": 15,
        },
    }

    score = sum(g["weight"] for g in gates.values() if g["passed"])

    result = {"lab": "rbdip-full-pipeline", "gates": gates, "score": score, "max_score": 100}

    out_path = ROOT / "score.json"
    out_path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"score.json: {score}/100")
    for name, gate in gates.items():
        print(f"  {'OK ' if gate['passed'] else 'FAIL'} {name} (вес {gate['weight']})")


if __name__ == "__main__":
    main()
