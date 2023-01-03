#import <XCTest/XCTest.h>
#import "Common.h"

extern "C" RUNTIME_USED id Konan_create_testSuite();

@interface XCTestLauncher : XCTestCase
@end

@implementation XCTestLauncher
// This is a starting point for XCTest to get the test suite with test cases
+ (id)defaultTestSuite {
    return Konan_create_testSuite();
}
@end
