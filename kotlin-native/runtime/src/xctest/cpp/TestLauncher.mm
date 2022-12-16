#import <XCTest/XCTest.h>
#import "Common.h"

@interface XCTestLauncher : XCTestCase
@end

@implementation XCTestLauncher

extern "C" RUNTIME_USED XCTestSuite* Konan_create_testSuite();

// This is a starting point for XCTest to get the test suite with test cases
+ (id)defaultTestSuite {
    return Konan_create_testSuite();
}

@end
